# LNBTPS (Land Nested Building Trade Point SD)
필지 단위 시도별 인덱스 - LNBTP의 시도(SD) 기준 17개 분할 버전

## 인덱스 정보
| Key          | Value                                                              |
|--------------|--------------------------------------------------------------------|
| IndexPrefix  | `lnbtps`                                                           |
| Indices      | `lnbtps_11`, `lnbtps_26`, `lnbtps_27`, ... (17개)                  | 시도코드별
| QueryPattern | `lnbtps_*`                                                         | 와일드카드
| Shards       | 4                                                                  | 인덱스당
| Replicas     | 0                                                                  |
| Distribution | SD-based (17 indices)                                              | 시도별

## 시도 코드 목록 (17개)
| Code | Name |
|------|------|
| 11   | 서울 |
| 26   | 부산 |
| 27   | 대구 |
| 28   | 인천 |
| 29   | 광주 |
| 30   | 대전 |
| 31   | 울산 |
| 36   | 세종 |
| 41   | 경기 |
| 42   | 강원 |
| 43   | 충북 |
| 44   | 충남 |
| 45   | 전북 |
| 46   | 전남 |
| 47   | 경북 |
| 48   | 경남 |
| 50   | 제주 |

## 다른 파티션 방식과 비교
| Feature      | LNBTPP       | LNBTPU       | LNBTPS         |
|--------------|--------------|--------------|----------------|
| IndexPattern | `lnbtp_*`    | `lnbtpu_*`   | `lnbtps_*`     |
| Partition    | EMD hash % 4 | PNU hash % 4 | SD code (17)   | 파티션 기준
| Count        | 4            | 4            | 17             | 인덱스 수
| Size         | Uneven       | Even         | SD population  | 불균등 (인구비례)
| Locality     | EMD          | None         | SD             | 지역성

## 파티션 알고리즘
```kotlin
// EMD 코드 앞 2자리 = SD 코드
val sdCode = emdCode.substring(0, 2)
val indexName = "lnbtps_$sdCode"

// 예: EMD=11110101 → SD=11 → lnbtps_11
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
    { "mgmBldrgstPk": "11110-100012345", "mainPurpsCdNm": "단독주택", "useAprDay": "2020-05-15", "totArea": 150.5 }
  ],
  "trades": [
    { "property": "LAND", "contractDate": "2024-01-15", "effectiveAmount": 500000000, "landAmountPerM2": 1512000 }
  ]
}
```

## API 엔드포인트

### 관리 API
| Method | Endpoint                    |
|--------|-----------------------------|
| PUT    | `/api/es/lnbtps/reindex`    | 전체 재인덱싱 (비동기)
| PUT    | `/api/es/lnbtps/forcemerge` | 세그먼트 병합 (비동기)
| GET    | `/api/es/lnbtps/count`      | 인덱스 문서 수 (시도별)
| DELETE | `/api/es/lnbtps`            | 인덱스 삭제

### 집계 API
| Method | Endpoint                  |
|--------|---------------------------|
| GET    | `/api/es/lnbtps/agg/sd`   | 시도별 집계
| GET    | `/api/es/lnbtps/agg/sgg`  | 시군구별 집계
| GET    | `/api/es/lnbtps/agg/emd`  | 읍면동별 집계

### 웹 페이지
| Method | Endpoint                   |
|--------|----------------------------|
| GET    | `/page/es/lnbtps/agg/sd`   | 시도별 집계 지도
| GET    | `/page/es/lnbtps/agg/sgg`  | 시군구별 집계 지도
| GET    | `/page/es/lnbtps/agg/emd`  | 읍면동별 집계 지도

## 인덱싱 방식

### 비동기 처리
```kotlin
CoroutineScope(indexingDispatcher).launch {
    // 인덱싱 로직
}
```

### 처리 흐름
```
[1] ensureAllIndicesExist()
    └─ 17개 인덱스 생성 (lnbtps_11, lnbtps_26, ...)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD 앞 2자리 → SD 코드 추출
    ├─ 해당 SD 인덱스로 bulk indexing
    └─ lnbtps_{sd} 인덱스에 저장
           ↓
[4] Forcemerge (17개 인덱스)
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
[LNBTPS] Worker-0 벌크 #1/87,748: 1,000/87,747,632 (0.0%) EMD=11110101 (1/377) → SD=11, 건물 1,234건, 실거래 567건, 스텝 1,234ms, 누적 2.34s
```

## 집계 쿼리

### 필수 설정
```kotlin
val response = esClient.search({ s ->
    s.index("lnbtps_*")  // 와일드카드로 17개 인덱스 동시 조회
        .size(0)
        .profile(true)   // 필수!
        .query { ... }
        .aggregations("by_region") { ... }
}, Void::class.java)
```

### 필터 없는 경우
LSRC (고정형 클러스터)에서 미리 집계된 결과 반환 → 초고속

### 필터 있는 경우
17개 인덱스 동시 쿼리 후 집계 → 시도별 분리로 특정 지역 쿼리 시 최적화 가능

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
| Document        | `es/document/land/LnbtpsDocument.kt`            |
| Indexing        | `es/service/lnbtps/LnbtpsIndexingService.kt`    |
| Aggregation     | `es/service/lnbtps/LnbtpsAggregationService.kt` |
| REST Controller | `controller/rest/LnbtpsRestController.kt`       |
| REST Agg        | `controller/rest/LnbtpsAggRestController.kt`    |
| Web Controller  | `controller/web/LnbtpsAggWebController.kt`      |
| Template        | `templates/es/lnbtps/agg.ftl`                   |
| Docker          | `docker-compose.lnbtps.yml`                     | volume: `geo_poc_os_lnbtps`
