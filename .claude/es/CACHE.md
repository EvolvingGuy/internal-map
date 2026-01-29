# OpenSearch 캐시 구조

OpenSearch는 3개 레이어의 캐시를 사용한다.
각각 역할과 위치가 다르며, 성능에 미치는 영향도 다르다.

## 캐시 레이어 요약

| Layer               | Location      | Default Size    | Eviction |
|---------------------|---------------|-----------------|----------|
| Request Cache       | JVM Heap      | heap 1%         | LRU
| File System Cache   | OS RAM        | available RAM   | OS auto
| Fielddata / Ordinals| JVM Heap      | unbounded       | circuit breaker

---

## 1. Request Cache

집계(aggregation) 결과를 **샤드 레벨**로 캐싱한다.
`size=0`인 쿼리(문서 반환 없이 집계만)에 자동 적용된다.

- 동일한 쿼리 + 동일한 샤드 데이터 → 캐시 히트 → 계산 없이 즉시 반환
- 엔트리당 수 KB (버킷 수십 개 × 코드/count/좌표)
- heap 1% = 80MB 기준 → 수만 개 엔트리 저장 가능
- LRU 방식으로 오래된 엔트리부터 밀려남
- 인덱스에 refresh/write가 발생하면 해당 샤드 캐시 무효화

### 캐시가 안 되는 경우

- `profile(true)` → **request_cache 강제 비활성**
- `request_cache=false` 파라미터
- `size > 0` (문서를 반환하는 쿼리)
- `now` 등 비결정적 함수 포함 쿼리

### 우리 프로젝트 적용

profile(true)를 환경변수로 전환하여 기본 비활성 처리 완료.
이후 동일 뷰포트+필터 조합의 반복 요청은 2~4ms로 응답한다.

---

## 2. File System Cache (OS Page Cache)

OS가 디스크 파일을 RAM에 자동 캐싱하는 레이어.
Lucene 세그먼트 파일(.fdt, .fdx, .doc, .pos 등)이 대상이다.

- JVM 바깥 메모리 사용 → heap과 무관
- 첫 쿼리(cold)가 느린 이유: 세그먼트 파일을 디스크에서 RAM으로 로딩
- 이후 같은 세그먼트 접근 시 RAM에서 읽음 (warm)
- OS가 메모리 압박 시 자동 eviction
- JVM heap을 너무 크게 잡으면 OS cache 공간이 줄어서 오히려 성능 저하

### 권장

총 RAM의 50%를 JVM heap, 나머지 50%를 OS cache로 남기는 것이 일반적 가이드라인.
(예: 16GB 노드 → heap 8GB, OS cache 8GB)

---

## 3. Fielddata / Global Ordinals

keyword 필드의 terms aggregation 등에서 사용하는 in-memory 구조.

- 세그먼트별로 생성 → forcemerge로 1 세그먼트면 1벌만 생성
- `eager_global_ordinals: true` 설정 시 refresh 시점에 미리 빌드
- JVM heap에 상주하며, circuit breaker로 과도한 사용 방지
- 우리 프로젝트: sd, sgg, emd 필드가 terms agg 대상

---

## 캐시 확인 방법

```bash
# request_cache 상태
GET /{index}/_stats/request_cache

# fielddata 상태
GET /{index}/_stats/fielddata

# 캐시 초기화
POST /{index}/_cache/clear
```

## 캐시 레이어 간 관계

```
요청 → Request Cache (히트 시 즉시 반환, 수ms)
       ↓ 미스
       Lucene 검색 실행
       → File System Cache (히트 시 RAM에서 세그먼트 읽기)
         ↓ 미스
         디스크 I/O (cold 쿼리, 가장 느림)
       → Fielddata/Ordinals (terms agg 시 heap에서 조회)
```