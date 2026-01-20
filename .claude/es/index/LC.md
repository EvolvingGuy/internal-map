# LC (Land Compact)
필지 단위 인덱스 - 비즈니스 필터 적용 가능, geo_shape intersects 쿼리 지원

## 인덱스 정보
| key        | value          |
|------------|----------------|
| IndexName  | `land_compact` |
| Shards     | 5              |
| Replicas   | 0              |

### 매핑
```json
{
  "pnu": "keyword",
  "sd": "keyword (eagerGlobalOrdinals)",
  "sgg": "keyword (eagerGlobalOrdinals)",
  "emd": "keyword (eagerGlobalOrdinals)",
  "land": {
    "jiyukCd1": "keyword",
    "jimokCd": "keyword",
    "area": "double",
    "price": "long",
    "center": "geo_point",
    "geometry": "geo_shape"
  },
  "building": {
    "mgmBldrgstPk": "keyword",
    "mainPurpsCdNm": "keyword",
    "regstrGbCdNm": "keyword",
    "pmsDay": "date",
    "stcnsDay": "date",
    "useAprDay": "date",
    "totArea": "scaled_float (100)",
    "platArea": "scaled_float (100)",
    "archArea": "scaled_float (100)"
  },
  "lastRealEstateTrade": {
    "property": "keyword",
    "contractDate": "date",
    "effectiveAmount": "long",
    "buildingAmountPerM2": "scaled_float (100)",
    "landAmountPerM2": "scaled_float (100)"
  }
}
```

## API 엔드포인트
| Method | Endpoint                 |
|--------|--------------------------|
| PUT    | `/api/es/lc/reindex`     | 전체 재인덱싱
| PUT    | `/api/es/lc/forcemerge`  | 세그먼트 병합
| GET    | `/api/es/lc/count`       | 인덱스 문서 수
| DELETE | `/api/es/lc`             | 인덱스 삭제

## 인덱싱 방식 비교 검토

### 검토 대상
| Approach              |
|-----------------------|
| A. Cursor Batch       | PNU 범위 기반 커서 조회, 배치 단위 처리
| B. Stream + IN        | 워커가 담당 SGG들을 IN절로 묶어 Stream 조회
| C. Stream + SGG Loop  | SGG 단위로 Stream 조회, 워커 내 루프
| D. Stateless Chunk    | 전체 PNU 목록 → 청크 단위 독립 처리

### 비교 분석
| Approach            | Complexity | Memory   | Transaction    | Recovery     | Partial Update |
|---------------------|------------|----------|----------------|--------------|----------------|
| A. Cursor Batch     | Medium     | Good     | Per Batch      | Per Batch    | Hard           |
| B. Stream + IN      | Low        | Good     | Per Worker     | Per Worker   | Hard           |
| **C. Stream + SGG** | **Low**    | **Good** | **Per SGG**    | **Per SGG**  | **Easy**       |
| D. Stateless        | High       | Best     | Independent    | Fine         | Easy           |

### 최종 채택: C. Stream + SGG 단위 루프

**채택 이유:**
1. **코드 단순화**: JPA Stream API 활용, 커서 상태 관리 불필요
2. **메모리 효율**: fetch size로 DB 커서 제어
3. **부분 갱신 확장성**: 추후 특정 시군구만 개별 업데이트 지원 가능
   - 인덱싱 전용 프로젝트 분리 시 활용 용이
4. **사람이 이해하기 쉬운 단위**: 시군구 코드는 비즈니스적으로 명확한 경계
5. **6개월 1회성 작업**: 복잡한 에러 복구 불필요, 단순함 우선

## Reindex 처리 순서
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:1, replica:0)
           ↓
[2] SGG 분배
    findDistinctSggCodes() → 20개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (20 Worker)
    각 워커가 담당 SGG 목록을 순회 (transactionTemplate 사용)
    ├─ SGG별 Stream 조회 (fetch size 5,000)
    ├─ 청크 단위로 building / trade 조회
    ├─ LandCompactDocument 생성
    ├─ ES bulk indexing (5,000건)
    └─ entityManager.clear() (메모리 관리)
           ↓
[4] Forcemerge
    maxNumSegments(1) → 세그먼트 병합
           ↓
[5] 결과 반환
    { totalCount, processed, indexed, elapsedMs, success }
```

## 병렬 처리 구조
| Config     | Value     |
|------------|-----------|
| Worker     | 20        | SGG 라운드로빈 분배
| Fetch Size | 5,000     | JPA Stream 힌트
| Bulk Size  | 5,000     | ES bulk 단위
| Dispatcher | IO        | 코루틴 디스패처
| Memory     | `clear()` | 배치마다 PC 클리어

### SGG 분배 전략
```kotlin
// SGG 코드 목록 조회 → 20개 워커에 라운드로빈 분배
val sggCodes = landCharRepo.findDistinctSggCodes()  // ~250개
val workerSggMap = sggCodes.withIndex()
    .groupBy { it.index % WORKER_COUNT }
    .mapValues { entry -> entry.value.map { it.value } }
// 워커 0: [11110, 11170, ...], 워커 1: [11140, 11200, ...], ...
```

### 워커 처리 로직
```kotlin
// SGG 단위로 트랜잭션 분리 (TransactionTemplate 사용)
for (sggCode in mySggCodes) {
    transactionTemplate.execute { _ ->
        repo.streamBySggCode(sggCode).use { stream ->
            stream.asSequence()
                .chunked(BATCH_SIZE)
                .forEach { batch ->
                    val docs = processBatch(batch)  // building, trade 조회 포함
                    bulkIndex(docs)
                    entityManager.clear()  // 메모리 관리
                }
        }
    }
}
```

## 공간 쿼리: geo_shape intersects
뷰포트(bbox)와 필지 폴리곤이 교차하는 모든 필지 조회.

```kotlin
// envelope GeoJSON: [[left, top], [right, bottom]]
val envelopeJson = mapOf(
    "type" to "envelope",
    "coordinates" to listOf(listOf(swLng, neLat), listOf(neLng, swLat))
)

bool.must { must ->
    must.geoShape { geo ->
        geo.field("land.geometry")
            .shape { shape ->
                shape.shape(JsonData.of(envelopeJson))
                    .relation(GeoShapeRelation.Intersects)
            }
    }
}
```

**장점:**
- 뷰포트 경계에 걸친 필지도 포함
- center 기반 bbox 쿼리 대비 정확한 결과

**단점:**
- center 기반 bbox 쿼리 대비 약간 느림

## Aggregation 서비스

### 행정구역별 집계 (LcAggregationService)
```
/api/es/lc/agg/sd   → sd 필드로 terms aggregation
/api/es/lc/agg/sgg  → sgg 필드로 terms aggregation
/api/es/lc/agg/emd  → emd 필드로 terms aggregation
```

### 그리드 집계 (LcGridAggregationService)
```
/api/es/lc/agg/grid → scripted terms aggregation (그리드 셀 계산)
```

**공통:**
- geo_shape intersects로 bbox 필터링
- geo_centroid sub-aggregation으로 중심점 계산
- LcAggFilter로 비즈니스 필터 적용

## 비즈니스 필터 (LcAggFilter)

### Building
| Param                    | Type       |
|--------------------------|------------|
| buildingMainPurpsCdNm    | List       | 주용도
| buildingRegstrGbCdNm     | List       | 등기구분
| buildingPmsDayRecent5y   | Boolean    | 최근5년 허가
| buildingStcnsDayRecent5y | Boolean    | 최근5년 착공
| buildingUseAprDayStart   | Int        | 준공년도 시작
| buildingUseAprDayEnd     | Int        | 준공년도 종료
| buildingTotAreaMin/Max   | BigDecimal | 연면적 범위
| buildingPlatAreaMin/Max  | BigDecimal | 대지면적 범위
| buildingArchAreaMin/Max  | BigDecimal | 건축면적 범위

### Land
| Param            | Type   |
|------------------|--------|
| landJiyukCd1     | List   | 용도지역
| landJimokCd      | List   | 지목
| landAreaMin/Max  | Double | 토지면적 범위
| landPriceMin/Max | Long   | 공시지가 범위

### Trade
| Param                           | Type       |
|---------------------------------|------------|
| tradeProperty                   | List       | 구분 (토지/건물)
| tradeContractDateStart/End      | LocalDate  | 거래일 범위
| tradeEffectiveAmountMin/Max     | Long       | 거래가 범위
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물면적당단가 범위
| tradeLandAmountPerM2Min/Max     | BigDecimal | 토지면적당단가 범위

## 스트리밍 & 메모리 관리

### 요약
- **JPA Stream + 커서 기반 조회**: fetchSize 힌트로 DB 커서 제어
- **트랜잭션 필수**: PostgreSQL JDBC 스트리밍은 `autoCommit=false` 필요 + MVCC 스냅샷 격리
- **메모리 관리**: 배치 처리 후 `entityManager.clear()`로 1차 캐시 비움
- **현재: Entity 사용** (개발 편의성), **프로덕션: DTO 전환 권장** (메모리 효율)

```kotlin
transactionTemplate.execute { _ ->
    repo.streamBySggCode(sggCode).use { stream ->
        stream.asSequence()
            .chunked(BATCH_SIZE)
            .forEach { batch ->
                val docs = processBatch(batch)
                bulkIndex(docs)
                entityManager.clear()  // 메모리 관리
            }
    }
}
```

> 상세 분석은 [별첨: 스트리밍 메모리 관리 상세](#별첨-스트리밍-메모리-관리-상세) 참조

## Bulk Size 분석

### 문서 크기 산정
```
LandCompactDocument 필드별 크기:
├── pnu: String (~20 bytes)
├── sd/sgg/emd: String (~12 bytes)
├── land: (~200 bytes)
│   ├── jiyukCd1, jimokCd, area, price, center
│   └── geometry: geo_shape (~100-500 bytes, 폴리곤 크기에 따라)
├── building: (~80 bytes, nullable)
└── lastRealEstateTrade: (~40 bytes, nullable)
→ 평균: ~400-600 bytes
```

### ES 권장 기준 (ES_GUIDELINE.md 참조)
| Avg Doc Size | Bulk Size | Payload |
|--------------|-----------|---------|
| ~500 bytes   | 5,000     | ~2.5MB  |
| ~1KB         | 5,000     | ~5MB    |

**ES 권장 Payload:** 5-15MB per bulk request

### 결정: 5,000건
- LandCompactDocument ≈ 500 bytes (평균)
- 5,000건 × 500 bytes = **~2.5MB per bulk**
- ES 권장 범위(5-15MB) 내, 효율적 처리
- Stream fetch size와 동일하게 맞춤

### 전체 규모
- 전체 문서 수: ~38,000,000개 (약 4천만 필지)
- 문서당 크기: ~500 bytes
- 예상 인덱스 크기: ~19GB

## 데이터 흐름

```
PostgreSQL (land_characteristic + building + real_estate_trade)
    │
    │  20 Worker 병렬 처리
    │  SGG 단위 Stream 조회 (fetch size 5,000)
    ↓
LandCompactDocument 생성
    │  - pnu, sd, sgg, emd
    │  - land (center, geometry as geo_shape)
    │  - building (nullable)
    │  - lastRealEstateTrade (nullable)
    ↓
ES Bulk Indexing
    │
    │  bulk 5,000건씩
    ↓
Elasticsearch (land_compact 인덱스)
    │
    │  forcemerge
    ↓
조회 준비 완료
```

## 관련 파일
- Controller: `controller/rest/LcRestController.kt`
- Indexing: `es/service/LcIndexingService.kt`
- Query: `es/service/LandCompactQueryService.kt`
- Aggregation: `es/service/LcAggregationService.kt`
- Grid Agg: `es/service/LcGridAggregationService.kt`
- Document: `es/document/land/LandCompactDocument.kt`
- Repository: `jpa/repository/LandCharacteristicRepository.kt`

---

## 별첨: 스트리밍 메모리 관리 상세

### PostgreSQL JDBC 스트리밍 요구사항

| Condition              | Required     |
|------------------------|--------------|
| `autoCommit = false`   | **Required** | 미충족 시 fetchSize 무시, 전체 ResultSet 로드
| `fetchSize` setting    | Required     | 한 번에 가져올 row 수 힌트
| Transaction maintained | Required     | 커서 유지 + MVCC 스냅샷 격리

### 트랜잭션이 필요한 이유

1. **기술적 요구**: PostgreSQL JDBC fetchSize는 `autoCommit=false`에서만 동작
2. **데이터 일관성**: MVCC 스냅샷 격리로 스트리밍 중 일관된 뷰 보장

### Persistence Context와 메모리

**문제**: JPA Stream으로 조회해도 엔티티가 1차 캐시(Persistence Context)에 누적 → OOM 위험

**해결**: 배치 처리 후 `entityManager.clear()` 호출

**메모리 패턴 (Sawtooth)**:
```
메모리
  ^
  |    /\    /\    /\
  |   /  \  /  \  /  \
  |  /    \/    \/    \
  +------------------------> 시간
       ↑      ↑      ↑
     clear  clear  clear
```

### Entity vs DTO Projection

| Aspect              | Entity | DTO Projection |
|---------------------|--------|----------------|
| Persistence Context | Used   | Not Used       | 메모리 누적 vs 미사용
| `clear()` Required  | O      | X              |
| Dirty Checking      | O      | X              | readOnly로 비활성화 가능
| Complexity          | Low    | Medium         | 매핑 필요
| GC Pressure         | High   | Low            |

**현재 선택: Entity** - 개발 편의성, POC 단계
**프로덕션 권장: DTO Projection** - Persistence Context 미사용으로 메모리 효율 최적

### fetchSize와 chunked의 관계

```
[JDBC Buffer]          [Stream iterate]         [Persistence Context]
  raw rows      →→→     Entity 생성      →→→      Entity 저장
  (byte[])              (hydration)               (managed)
     ↑                       ↑                        ↑
 fetchSize로            next() 호출 시점에         이 시점에 PC 진입
 미리 당겨옴             Entity로 변환
```

| Layer              |
|--------------------|
| `fetchSize` (JDBC) | DB → 드라이버 네트워크 전송 단위 (힌트, 정확하지 않음)
| `chunked` (Kotlin) | 스트림 요소를 List로 그룹핑 (정확히 N개)

### fetchSize ≠ chunked 가능성

fetchSize는 힌트일 뿐, 정확히 일치하지 않을 수 있음. 그러나 **clear()는 안전**:

- Java Stream은 **lazy/pull-based** 구조
- JDBC 버퍼에 raw rows가 있어도, **iterate 하기 전까지 Entity로 hydration 안 됨**
- `clear()`는 PC만 비움 → JDBC 버퍼의 미소비 raw data는 영향 없음
- 다음 iterate 시 fresh하게 Entity 생성 → 새 PC에 저장

### 근거

1. **Hydration 시점**: Entity 생성은 ResultSet 순회 시점에 발생
   > "The act of converting the Object[] loaded state into a Java entity object is referred to as hydration."

2. **PC 진입 시점**: Stream 처리한 만큼만 PC에 쌓임
   > "Even though we are streaming... Hibernate keeps track of all entities in its persistence context."

3. **Stream 특성**: Java Stream은 lazy/pull-based → terminal operation 전까지 실제 처리 안 함

### 실제 동작 확인

- 메모리 그래프: Sawtooth 패턴으로 안정적 유지
- clear() 후 GC 발생, 바닥선 일정 → 누수 없음
- fetchSize와 chunked 경계가 맞지 않아도 정상 동작 확인

### 참고 자료

- [Vlad Mihalcea - JDBC Statement fetchSize](https://vladmihalcea.com/resultset-statement-fetching-with-jdbc-and-hibernate/)
- [Entity Hydration 설명](https://codingtechroom.com/question/hydrating-jpa-hibernate-entities)
- [Spring Data JPA Streaming](https://medium.com/predictly-on-tech/spring-data-jpa-batching-using-streams-af456ea611fc)
