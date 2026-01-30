# Cache Layers in OpenSearch/Elasticsearch

## Summary

| Mode | OS Page Cache | Node Query Cache | Shard Request Cache | Fielddata Cache |
|------|---------------|------------------|---------------------|-----------------|
| `profile=true`            | O | X | X | O |
| `request_cache=false`     | O | O | X | O |
| default (all enabled)     | O | O | O | O |

- **`profile=true`**: request cache + query cache 모두 비활성화. 프로파일링은 쿼리 내부 단계별 소요시간을 측정하므로, 캐시가 개입하면 수치가 왜곡됨. OS page cache와 fielddata(global ordinals)는 여전히 동작. 알 수 있는 것: 쿼리/집계의 단계별 소요시간(create_weight, build_scorer, collect 등), 병목 구간 식별.
- **`request_cache=false`**: shard-level request cache만 비활성화. node query cache(filter bitset)는 여전히 활성화되어 필터 캐시의 도움을 받음. OS page cache도 동작. 동일 쿼리 반복 시 request cache 히트를 방지하여 실제 검색 엔진의 처리 성능을 측정 가능.
- **default**: 전체 캐시 스택 활성화. `size:0` 쿼리(집계)는 shard request cache에 자동 캐싱. 동일 쿼리 반복 시 수ms 내 즉시 반환.

---

## Cache Layers (Detail)

### 1. OS Filesystem / Page Cache

가장 하위 레이어. OS가 관리하며 ES/OpenSearch가 직접 제어할 수 없음.

- Lucene segment 파일을 `mmap`으로 메모리 매핑하여 디스크 I/O 없이 접근
- segment는 immutable(불변)이므로 캐시 친화적 - dirty write-back 없음
- JVM heap을 총 RAM의 50% 이하로 설정하여 나머지를 page cache에 양보하는 것이 권장
- **성능 차이**: Elastic 자체 테스트 기준 캐시 히트 vs 미스 약 **10배** 차이
- **invalidation**: OS의 LRU 정책. 메모리 압박 시 오래된 페이지부터 evict
- **preload 설정**: `index.store.preload`로 특정 파일 확장자를 사전 로드 가능 (`nvd`, `dvd`, `tim`, `doc`, `dim` 등)

### 2. Node-Level Query Cache (Filter Cache)

filter context로 실행된 쿼리 결과를 **bitset**(문서당 1bit)으로 캐싱.

- **scope**: 노드 단위. 해당 노드의 모든 샤드가 공유
- **동작**: 첫 실행 시 segment를 파싱해서 매칭 결과를 BitSet으로 저장. 이후 동일 필터 → BitSet 재사용
- **다중 필터**: 여러 BitSet에 대해 bitwise AND/OR 연산 → Lucene 구조 재파싱보다 수 자릿수(orders of magnitude) 빠름
- **segment 단위 저장**: segment가 immutable이므로 기존 BitSet은 invalidation 불필요. 새 segment만 새로 빌드
- **캐싱 조건** (모두 충족해야 캐싱):
  - segment 내 문서 10,000건 이상
  - segment가 전체 샤드의 3% 이상
  - 일정 횟수 이상 사용된 필터
  - filter context에서 실행 (must/should 등 scoring context 제외)
- **default**: 10% heap, 최대 10,000 entries
- **eviction**: LRU
- **비활성화 조건**: `profile=true` 시 비활성화

### 3. Shard-Level Request Cache

`size:0` 쿼리(집계, count, suggestion)의 **직렬화된 전체 응답**을 샤드별로 캐싱.

- **scope**: 샤드 단위 캐시, 노드 레벨에서 메모리 공유
- **cache key 구성**:
  1. `IndexShardCacheEntity` - 샤드 참조
  2. `ReaderCacheKeyId` - 샤드 상태의 고유 ID (변경 시 갱신)
  3. `BytesReference` - 쿼리 바이트 직렬화
- **default 캐싱 대상**: `size:0`인 요청만 자동 캐싱. hits 반환 요청은 명시적 `request_cache=true` 필요
- **invalidation**: 샤드 refresh 시 데이터 변경이 있으면 해당 샤드의 **모든** 캐시 엔트리 무효화. 문서 단위가 아닌 샤드 단위 invalidation
- **default size**: 1% heap (`indices.requests.cache.size`)
- **캐싱 불가 쿼리**:
  - `profile: true`
  - scroll 쿼리
  - `now` 등 상대 시간 표현식 포함 쿼리
  - DFS 쿼리
  - 비결정적 쿼리 (`Math.random()` 등)
- **eviction**: LRU

### 4. Fielddata Cache / Global Ordinals

집계·정렬에 사용되는 데이터 구조 캐시. `keyword`, `ip`, `flattened` 필드의 global ordinals를 저장.

- **global ordinals**: segment별 ordinal(문자열→정수) 매핑을 샤드 전체로 통합. 문자열 비교 대신 정수 비교로 집계 수행
- **lazy build (default)**: 첫 집계 쿼리 시 빌드. 첫 요청이 느림
- **`eager_global_ordinals: true`**: refresh 시마다 사전 빌드. refresh는 느려지지만 쿼리 시 즉시 사용 가능
- **forcemerge 1 segment**: segment가 1개면 segment ordinal = global ordinal. 추가 매핑 불필요 → 성능 이점
- **default size**: 무제한 (circuit breaker로 OOM 방지)
- **비활성화 조건**: `profile=true`에도 비활성화되지 않음
- 우리 프로젝트: `sd`, `sgg`, `emd` 필드가 terms agg 대상

---

## Profile Breakdown Phases

`profile=true` 시 각 쿼리/집계의 내부 단계별 소요시간을 확인할 수 있음.

### Query Phases

| Phase | Description |
|-------|-------------|
| `create_weight`  | (IndexSearcher, Query) 튜플의 임시 컨텍스트 생성 |
| `build_scorer`   | 매칭 문서를 순회하는 Scorer 생성. 가장 비용이 큰 단계 |
| `next_doc`       | 다음 매칭 문서 ID 탐색 |
| `advance`        | skip 기반 문서 탐색 (conjunction에서 주로 사용) |
| `match`          | 2-phase 검증 (phrase query 등에서 근사 매칭 후 정확 확인) |
| `score`          | 개별 문서 스코어링 |

### Aggregation Phases

| Phase | Description |
|-------|-------------|
| `initialize`           | 집계 초기화 |
| `build_leaf_collector` | segment별 수집 구조 준비 |
| `collect`              | 문서 수집 |
| `post_collection`      | 수집 후 정리 |
| `build_aggregation`    | 최종 집계 결과 생성 |

### Profile의 한계

네트워크 오버헤드, 큐 대기 시간, coordinating node에서의 샤드 응답 병합 시간, global ordinals 빌드 시간은 측정되지 않음.

---

## Cache Flow

```
요청 → Shard Request Cache (히트 시 즉시 반환, 수ms)
       ↓ 미스
       Lucene 검색 실행
       ├─ Node Query Cache (filter bitset 히트 시 재파싱 없이 BitSet 사용)
       ├─ OS Page Cache (히트 시 RAM에서 세그먼트 읽기, 미스 시 디스크 I/O)
       └─ Fielddata/Ordinals (terms agg 시 heap에서 조회)
```

## Cache 확인 방법

```bash
# request_cache 상태
GET /{index}/_stats/request_cache

# fielddata 상태
GET /{index}/_stats/fielddata

# node query cache 상태
GET /_nodes/stats/indices/query_cache

# 캐시 초기화
POST /{index}/_cache/clear
```

---

## Sources

- [OpenSearch - Index Request Cache](https://docs.opensearch.org/latest/search-plugins/caching/request-cache/)
- [OpenSearch - Caching Overview](https://docs.opensearch.org/latest/search-plugins/caching/index/)
- [OpenSearch Blog - Optimizing Query Performance](https://opensearch.org/blog/understanding-index-request-cache/)
- [OpenSearch - Field Data Cache](https://docs.opensearch.org/latest/search-plugins/caching/field-data-cache/)
- [Elasticsearch - Shard Request Cache](https://www.elastic.co/guide/en/elasticsearch/reference/8.18/shard-request-cache.html)
- [Elasticsearch - Node Query Cache Settings](https://www.elastic.co/docs/reference/elasticsearch/configuration-reference/node-query-cache-settings)
- [Elastic Blog - Caching Deep Dive](https://www.elastic.co/blog/elasticsearch-caching-deep-dive-boosting-query-speed-one-cache-at-a-time)
- [Elastic Blog - All About Filter BitSets](https://www.elastic.co/blog/all-about-elasticsearch-filter-bitsets)
- [Elasticsearch - Profile API](https://www.elastic.co/guide/en/elasticsearch/reference/8.18/search-profile.html)
- [GitHub PR #48195 - Disable Caching When Profiled](https://github.com/elastic/elasticsearch/pull/48195)
- [Elasticsearch - eager_global_ordinals](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/eager-global-ordinals)
- [Elasticsearch - Preload Data into File System Cache](https://www.elastic.co/guide/en/elasticsearch/reference/current/preload-data-to-file-system-cache.html)
