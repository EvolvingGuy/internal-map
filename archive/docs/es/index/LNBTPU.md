# LNBTPU (Land Nested Building Trade Point Uniform)
필지 단위 파티션 인덱스 - LNBTP의 4분할 균등분배 버전, PNU hash % 4로 문서 균등 분산

## 인덱스 정보
| Key          | Value                                          |
|--------------|------------------------------------------------|
| IndexPrefix  | `lnbtpu`                                       |
| Indices      | `lnbtpu_1`, `lnbtpu_2`, `lnbtpu_3`, `lnbtpu_4` |
| QueryPattern | `lnbtpu_*`                                     | 와일드카드
| Shards       | 4                                              | 파티션당
| Replicas     | 0                                              |
| Distribution | PNU hash % 4                                   | 균등분배

## LNBTPP와의 비교
| Feature      | LNBTPP             | LNBTPU             |
|--------------|--------------------|--------------------|
| IndexPattern | `lnbtp_*`          | `lnbtpu_*`         |
| Partition    | EMD hash % 4       | PNU hash % 4       | 비균등 vs 균등
| Locality     | Same EMD → Same P  | Random by PNU      | 지역성 vs 분산
| Size         | Uneven             | Even               | 파티션 크기
| Query        | EMD locality       | Full parallel      | 조회 특성

## 균등분배 알고리즘
```kotlin
// PNU 코드 기반 파티션 결정 (hash % 4 + 1)
fun getPartition(pnu: String): Int = (pnu.hashCode().absoluteValue % PARTITION_COUNT) + 1

// 인덱싱 시 각 배치별 파티션 분포 로그
// [LNBTPU] P1:248 P2:252 P3:245 P4:255
```

### 문서 구조 (Document)
```json
{
  "pnu": "1111010100100010000",
  "sd": "11",
  "sgg": "11110",
  "emd": "11110101",
  "land": {
    "jiyukCd1": "14",
    "jimokCd": "08",
    "area": 330.5,
    "price": 5200000,
    "center": { "lat": 37.5665, "lon": 126.9780 }
  },
  "buildings": [
    { "mgmBldrgstPk": "11110-100012345", "mainPurpsCdNm": "단독주택", "useAprDay": "2020-05-15", "totArea": 150.5 },
    { "mgmBldrgstPk": "11110-100012346", "mainPurpsCdNm": "제2종근린생활시설", "useAprDay": "2022-03-10", "totArea": 85.2 }
  ],
  "trades": [
    { "property": "LAND", "contractDate": "2024-01-15", "effectiveAmount": 500000000, "landAmountPerM2": 1512000 },
    { "property": "SINGLE", "contractDate": "2022-03-10", "effectiveAmount": 320000000, "buildingAmountPerM2": 2100000 }
  ]
}
```

### ES 매핑 정의 (Mapping)
```json
{
  "pnu": { "type": "keyword" },
  "sd": { "type": "keyword", "eager_global_ordinals": true },
  "sgg": { "type": "keyword", "eager_global_ordinals": true },
  "emd": { "type": "keyword", "eager_global_ordinals": true },
  "land": {
    "type": "object",
    "properties": {
      "jiyukCd1": { "type": "keyword" },
      "jimokCd": { "type": "keyword" },
      "area": { "type": "double" },
      "price": { "type": "long" },
      "center": { "type": "geo_point" }
    }
  },
  "buildings": {
    "type": "nested",
    "properties": {
      "mgmBldrgstPk": { "type": "keyword" },
      "mainPurpsCdNm": { "type": "keyword" },
      "regstrGbCdNm": { "type": "keyword" },
      "pmsDay": { "type": "date" },
      "stcnsDay": { "type": "date" },
      "useAprDay": { "type": "date" },
      "totArea": { "type": "scaled_float", "scaling_factor": 100 },
      "platArea": { "type": "scaled_float", "scaling_factor": 100 },
      "archArea": { "type": "scaled_float", "scaling_factor": 100 }
    }
  },
  "trades": {
    "type": "nested",
    "properties": {
      "property": { "type": "keyword" },
      "contractDate": { "type": "date" },
      "effectiveAmount": { "type": "long" },
      "buildingAmountPerM2": { "type": "scaled_float", "scaling_factor": 100 },
      "landAmountPerM2": { "type": "scaled_float", "scaling_factor": 100 }
    }
  }
}
```

## API 엔드포인트

### 관리 API
| Method | Endpoint                    |
|--------|-----------------------------|
| PUT    | `/api/es/lnbtpu/reindex`    | 전체 재인덱싱 (비동기)
| PUT    | `/api/es/lnbtpu/forcemerge` | 세그먼트 병합 (비동기)
| GET    | `/api/es/lnbtpu/count`      | 인덱스 문서 수 (파티션별)
| DELETE | `/api/es/lnbtpu`            | 인덱스 삭제

### 집계 API
| Method | Endpoint                  |
|--------|---------------------------|
| GET    | `/api/es/lnbtpu/agg/sd`   | 시도별 집계
| GET    | `/api/es/lnbtpu/agg/sgg`  | 시군구별 집계
| GET    | `/api/es/lnbtpu/agg/emd`  | 읍면동별 집계

### 웹 페이지
| Method | Endpoint                   |
|--------|----------------------------|
| GET    | `/page/es/lnbtpu/agg/sd`   | 시도별 집계 지도
| GET    | `/page/es/lnbtpu/agg/sgg`  | 시군구별 집계 지도
| GET    | `/page/es/lnbtpu/agg/emd`  | 읍면동별 집계 지도

## 인덱싱 방식

### 비동기 처리
```kotlin
// reindex, forcemerge 모두 비동기
CoroutineScope(indexingDispatcher).launch {
    // 인덱싱 로직
}
```

### 균등분배 처리 흐름
```
[1] ensureAllIndicesExist()
    └─ 4개 인덱스 생성 (lnbtpu_1 ~ lnbtpu_4)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회 (N건)
    ├─ trade 조회 (N건)
    ├─ LnbtpuDocument 생성
    ├─ PNU hash % 4로 파티션 그룹핑 ← 핵심 차이점
    └─ 파티션별 bulk indexing
           ↓
[4] Forcemerge (4개 인덱스)
           ↓
[5] 결과 반환
```

### 인덱싱 설정
| Config     | Value |
|------------|-------|
| Worker     | 10    | EMD 라운드로빈 분배
| Fetch Size | 1,000 | JPA Stream 힌트
| Bulk Size  | 1,000 | ES bulk 단위
| Dispatcher | IO    | 코루틴 디스패처

### 로그 형식
```
[LNBTPU] Worker-0 벌크 #1/87,748: 1,000/87,747,632 (0.0%) EMD=11110101 (1/377) → P1:248 P2:252 P3:245 P4:255, 건물 1,234건, 실거래 567건, 스텝 1,234ms, 누적 2.34s
```

## 집계 쿼리

### 필수 설정
```kotlin
val response = esClient.search({ s ->
    s.index("lnbtpu_*")  // 와일드카드로 4개 파티션 동시 조회
        .size(0)
        .profile(true)   // 필수!
        .query { ... }
        .aggregations("by_region") { ... }
}, Void::class.java)
```

### 필터 없는 경우
LSRC (고정형 클러스터)에서 미리 집계된 결과 반환 → 초고속

### 필터 있는 경우
4개 파티션 동시 쿼리 후 집계 → 균등 분배로 인한 병렬 처리 효율 극대화

## 비즈니스 필터 (LcAggFilter)

### Building (nested)
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

### Trade (nested)
| Param                           | Type       |
|---------------------------------|------------|
| tradeProperty                   | List       | 구분 (토지/건물)
| tradeContractDateStart/End      | LocalDate  | 거래일 범위
| tradeEffectiveAmountMin/Max     | Long       | 거래가 범위
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물면적당단가 범위
| tradeLandAmountPerM2Min/Max     | BigDecimal | 토지면적당단가 범위

## 관련 파일
| Type            | Path                                            |
|-----------------|-------------------------------------------------|
| Document        | `es/document/land/LnbtpuDocument.kt`            |
| Indexing        | `es/service/lnbtpu/LnbtpuIndexingService.kt`    |
| Aggregation     | `es/service/lnbtpu/LnbtpuAggregationService.kt` |
| REST Controller | `controller/rest/LnbtpuRestController.kt`       |
| REST Agg        | `controller/rest/LnbtpuAggRestController.kt`    |
| Web Controller  | `controller/web/LnbtpuAggWebController.kt`      |
| Template        | `templates/es/lnbtpu/agg.ftl`                   |
| Docker          | `docker-compose.lnbtpu.yml`                     | volume: `geo_poc_os_lnbtpu`
