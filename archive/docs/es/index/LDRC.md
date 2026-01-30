# LDRC (Land Dynamic Region Cluster)
동적 행정구역 클러스터 - 비즈니스 필터 없이 H3 기반으로 뷰포트에 따라 동적 집계

## 인덱스 정보
| key        | value                         |
|------------|-------------------------------|
| IndexName  | `land_dynamic_region_cluster` |
| Shards     | 3                             |
| Replicas   | 0                             |

### 매핑
level: SD, SGG, EMD
H3 Resolution: SD=5, SGG=7, EMD=10
```json
{
  "id": "keyword",
  "level": "keyword",
  "h3Index": "long",
  "regionCode": "long",
  "cnt": "integer",
  "sumLat": "double",
  "sumLng": "double",
  "sgg7": "long",
  "sd5": "long",
  "regionCodeSgg": "long",
  "regionCodeSd": "long"
}
```
**EMD 문서 전용 필드:**
- `sgg7`: SGG 집계용 H3 인덱스 (res 7) - `h3.cellToParent(h3Index, 7)`
- `sd5`: SD 집계용 H3 인덱스 (res 5) - `h3.cellToParent(h3Index, 5)`
- `regionCodeSgg`: SGG 집계용 regionCode (앞 5자리)
- `regionCodeSd`: SD 집계용 regionCode (앞 2자리)

## API 엔드포인트
| Method | Endpoint                   |
|--------|----------------------------|
| PUT    | `/api/es/ldrc/reindex`     | 전체 재인덱싱 (EMD→SGG→SD)
| PUT    | `/api/es/ldrc/reindex/emd` | EMD만 인덱싱
| PUT    | `/api/es/ldrc/reindex/sgg` | SGG만 인덱싱
| PUT    | `/api/es/ldrc/reindex/sd`  | SD만 인덱싱
| PUT    | `/api/es/ldrc/forcemerge`  | 세그먼트 병합
| GET    | `/api/es/ldrc/count`       | 인덱스 문서 수

## Reindex 처리 순서 (3-Level 체인)
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:3, replica:0)
           ↓
[2] EMD (Resolution 10) - PostgreSQL → ES (스트리밍)
    findAllBy() - 스트리밍 조회 (fetch size: 10,000)
    → h3.cellToParent로 sgg7, sd5 계산
    → 버퍼 10,000건마다 bulkIndex
    → 메모리: 최대 10,000건만 유지
    → 결과: ~5,000,000개 EMD 문서
           ↓
[3] SGG (Resolution 7) - ES Aggregation
    ES composite aggregation: sgg7 + regionCodeSgg 기준
    → sum(cnt), sum(sumLat), sum(sumLng)
    → bulkIndex
           ↓
[4] SD (Resolution 5) - ES Aggregation
    ES composite aggregation: sd5 + regionCodeSd 기준
    → sum(cnt), sum(sumLat), sum(sumLng)
    → bulkIndex
           ↓
[5] Forcemerge (비동기)
    maxNumSegments(1), waitForCompletion(false)
    → 백그라운드 실행 (타임아웃 방지)
           ↓
[6] 결과 반환
    { emd, sgg, sd, total, forcemerge: "async", success }
```

### 변경 사유: ES Aggregation 활용
**기존 방식 (search_after + app-side 집계):**
- ES에서 500만 건 순회 (500회 배치)
- app에서 h3.cellToParent 계산
- 네트워크 I/O 부담

**변경 방식 (ES Aggregation):**
- EMD 인덱싱 시 sgg7, sd5 미리 저장
- ES aggregation 쿼리 1회로 집계 완료
- 네트워크 I/O 최소화

**문서 크기 증가:**
- sgg7, sd5, regionCodeSgg, regionCodeSd 추가: +32 bytes (Long 4개)
- 100 bytes → 132 bytes (~32% 증가)
- 5M × 32 bytes = ~160MB 증가 → 허용 범위

### Forcemerge 비동기 처리
**문제:**
- 500만 문서 인덱스 forcemerge → 30초 이상 소요
- ES REST client 기본 타임아웃 30초 → `SocketTimeoutException`

**해결:**
```kotlin
esClient.indices().forcemerge { f ->
    f.index(INDEX_NAME)
        .maxNumSegments(1L)
        .waitForCompletion(false)  // 비동기
}
```
- 요청 즉시 반환, ES 백그라운드에서 실행
- 완료 확인: ES 서버 로그 또는 `_cat/segments` API

## H3 Resolution 매핑
| Level | Resolution | Cell Edge |
|-------|------------|-----------|
| EMD   | 10         | ~15m      | 읍면동 세밀 집계
| SGG   | 7          | ~1.2km    | 시군구 집계
| SD    | 5          | ~8km      | 시도 집계

## 핵심 테이블 (PostgreSQL)
```sql
-- EMD 집계 뷰 (사전 집계됨)
SELECT id, code, h3_index, cnt, sum_lat, sum_lng
FROM manage.r3_pnu_agg_emd_10;
```

**테이블 구조:**
- `code`: 행정구역 코드 (읍면동)
- `h3_index`: H3 인덱스 (res 10)
- `cnt`: 필지 수
- `sum_lat`: 위도 합
- `sum_lng`: 경도 합

## ES 집계 로직 (Composite Aggregation)
```kotlin
// SGG 집계: sgg7 + regionCodeSgg 기준 (Composite Aggregation)
val response = esClient.search({ s ->
    s.index(INDEX_NAME)
        .size(0)
        .query { q -> q.term { t -> t.field("level").value("EMD") } }
        .aggregations("composite_agg") { a ->
            a.composite { c ->
                c.size(10000)
                    .sources(listOf(
                        mapOf("h3" to CompositeAggregationSource.of { src -> src.terms { t -> t.field("sgg7") } }),
                        mapOf("region" to CompositeAggregationSource.of { src -> src.terms { t -> t.field("regionCodeSgg") } })
                    ))
            }
                .aggregations("sum_cnt") { sub -> sub.sum { sum -> sum.field("cnt") } }
                .aggregations("sum_lat") { sub -> sub.sum { sum -> sum.field("sumLat") } }
                .aggregations("sum_lng") { sub -> sub.sum { sum -> sum.field("sumLng") } }
        }
}, Void::class.java)

// 집계 결과 → LdrcDocument 변환
for (bucket in compositeAgg.buckets().array()) {
    val h3Index = bucket.key()["h3"]?.longValue() ?: continue
    val regionCode = bucket.key()["region"]?.longValue() ?: continue
    val cnt = bucket.aggregations()["sum_cnt"]?.sum()?.value()?.toInt() ?: 0
    // LdrcDocument 생성
}
```

**Composite Aggregation 사용 이유:**
- terms aggregation은 버킷 수 제한 있음 (기본 10,000)
- composite aggregation은 afterKey로 페이징 가능
- 대량 집계 시 안정적

## Bulk Size 분석

### 문서 크기 산정
```
LdrcDocument 필드별 크기:
├── id: String (~30 bytes)        "EMD_123456789012345_1234567890"
├── level: String (~3 bytes)      "EMD"
├── h3Index: Long (8 bytes)
├── regionCode: Long (8 bytes)
├── cnt: Int (4 bytes)
├── sumLat: Double (8 bytes)
├── sumLng: Double (8 bytes)
├── sgg7: Long (8 bytes)          EMD 전용
├── sd5: Long (8 bytes)           EMD 전용
├── regionCodeSgg: Long (8 bytes) EMD 전용
└── regionCodeSd: Long (8 bytes)  EMD 전용
→ EMD 합계: 약 120-140 bytes
→ SGG/SD 합계: 약 80-100 bytes (EMD 전용 필드 미포함)
```

### ES 권장 기준 (ES_GUIDELINE.md 참조)
| Avg Doc Size | Bulk Size | Payload |
|--------------|-----------|---------|
| ~100 bytes   | 10,000    | ~1MB    |
| ~500 bytes   | 5,000     | ~2.5MB  |
| ~1KB         | 5,000     | ~5MB    |

**ES 권장 Payload:** 5-15MB per bulk request

### 결정: 10,000건
- LdrcDocument ≈ 100 bytes
- 10,000건 × 100 bytes = **~1MB per bulk**
- ES 권장 범위(5-15MB) 하한선이지만, 문서 크기가 작아 적절
- 너무 크게 잡으면 메모리 부담, 너무 작으면 요청 오버헤드

### 전체 규모
- EMD 문서 수: ~5,000,000개
- 전체 페이로드: ~500MB
- 예상 벌크 횟수: ~500회

## EMD 조회 방식: 스트리밍

### 방안 비교
| Method        | Memory       | Query | Complexity | Connection |
|---------------|--------------|-------|------------|------------|
| findAll()     | X (all load) | 1     | low        | short      |
| Stream API    | O (chunk)    | 1     | low        | long       | 선택
| Cursor Paging | O (chunk)    | N     | medium     | short      |
| Slice Paging  | O (chunk)    | N     | low        | short      | offset 성능↓

### 결정: 스트리밍
**이유:**
1. **메모리 절약** - 5,000,000건 → 최대 10,000건만 메모리 유지
2. **단발성 배치 작업** - 커넥션 점유 길어도 무방
3. **단순 파이프라인** - 조회 → 변환 → 벌크 인덱싱, 복잡한 로직 없음
4. **쿼리 1회** - DB 부하 최소화
5. **코드 간결** - 커서 경계 관리 불필요

**커서가 부적합한 이유:**
- 페이지마다 쿼리 실행 오버헤드
- 커서 경계 관리 코드 복잡

### 구현
```kotlin
// Repository
@QueryHints(QueryHint(name = HINT_FETCH_SIZE, value = "10000"))
fun findAllBy(): Stream<PnuAggEmd10>

// Service - @Transactional(readOnly = true) 필수
emd10Repository.findAllBy().use { stream ->
    stream.forEach { entity ->
        buffer.add(LdrcDocument.fromEmd10(entity, h3))
        if (buffer.size >= 10000) {
            bulkIndex(buffer)
            buffer.clear()
        }
    }
}
```

**메모리 사용: 5,000,000건 → 최대 10,000건**

**필요 조건:**
- `@Transactional(readOnly = true)` → autoCommit=false 보장
- `@QueryHints(HINT_FETCH_SIZE)` → fetch size 설정
- JDBC URL 별도 설정 불필요 (최신 PostgreSQL 드라이버)

## 데이터 흐름

```
PostgreSQL (manage.r3_pnu_agg_emd_10)
    │
    │  findAllBy() - 스트리밍 조회 (10,000건씩 fetch)
    ↓
EMD 엔티티 (버퍼: 최대 10,000건)
    │
    │  LdrcDocument.fromEmd10(entity, h3)
    │  → h3.cellToParent로 sgg7, sd5 계산
    ↓
ES 인덱싱 (EMD, res 10 + sgg7 + sd5 + regionCodeSgg + regionCodeSd)
    │
    │  ES composite aggregation: sgg7 + regionCodeSgg 기준
    ↓
ES 인덱싱 (SGG, res 7)
    │
    │  ES composite aggregation: sd5 + regionCodeSd 기준
    ↓
ES 인덱싱 (SD, res 5)
    │
    │  forcemerge (비동기, 백그라운드)
    ↓
Elasticsearch (ldrc 인덱스)
```

## 조회 시 줌 레벨 제한
| Level | Min Zoom | Scale |
|-------|----------|-------|
| SD    | 0        | -     | 제한없음
| SGG   | 10       | 10km  |
| EMD   | 14       | 500m  |

## LSRC와의 차이점
| Category    | LSRC (Static)        | LDRC (Dynamic)        |
|-------------|----------------------|-----------------------|
| Data Change | viewport-independent | viewport-dependent    | 데이터 변동
| Indexing    | DB GROUP BY          | 3-Level + ES Agg      | 인덱싱 전략
| H3          | -                    | Resolution 5/7/10     |
| DB Query    | 1 (aggregate)        | 1 (EMD load)          |
| ES Query    | 0                    | 2 (SGG, SD agg)       | aggregation 활용
| Doc Count   | ~5,000               | ~5,000,000            | 문서 수
| Bulk Size   | 2,000                | 10,000                |

## 관련 파일
- Controller: `controller/rest/LdrcRestController.kt`
- Service: `es/service/LdrcIndexingService.kt`
- QueryService: `es/service/LdrcQueryService.kt`
- Document: `es/document/cluster/LdrcDocument.kt`
- Entity: `jpa/entity/PnuAggEmd10.kt`
- Repository: `jpa/repository/PnuAggEmd10Repository.kt`