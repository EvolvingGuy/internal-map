# LNB (Land Nested Building)
필지 단위 인덱스 - 건물을 nested array로 저장하여 개별 건물 단위 필터링 지원

## 인덱스 정보
| key        | value                   |
|------------|-------------------------|
| IndexName  | `land_nested_building`  |
| Shards     | 3                       |
| Replicas   | 0                       |

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
    "center": { "lat": 37.5665, "lon": 126.9780 },
    "geometry": { "type": "Polygon", "coordinates": [[...]] }
  },
  "buildings": [
    { "mgmBldrgstPk": "11110-100012345", "mainPurpsCdNm": "단독주택", "useAprDay": "2020-05-15", "totArea": 150.5 },
    { "mgmBldrgstPk": "11110-100012346", "mainPurpsCdNm": "제2종근린생활시설", "useAprDay": "2022-03-10", "totArea": 85.2 }
  ],
  "lastRealEstateTrade": {
    "property": "LAND",
    "contractDate": "2024-01-15",
    "effectiveAmount": 500000000,
    "landAmountPerM2": 1512000
  }
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
      "center": { "type": "geo_point" },
      "geometry": { "type": "geo_shape" }
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
  "lastRealEstateTrade": {
    "type": "object",
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

## LC와의 차이점

| Feature      | LC (Land Compact)       | LNB (Land Nested Building) |
|--------------|-------------------------|----------------------------|
| 건물 저장    | `building` (object, 1건)| `buildings` (nested, N건)  |
| 건물 필터    | cross-object matching   | 정확한 개별 건물 매칭       |
| 실거래 저장  | `lastRealEstateTrade`   | `lastRealEstateTrade`      |
| 쿼리 방식    | 일반 필터               | nested 쿼리                |

### nested 쿼리의 장점
- **정확한 필터링**: "준공일 2020년 이후 + 연면적 1000㎡ 이상 건물이 있는 필지" 정확 매칭
- LC의 object 타입은 조건이 다른 건물에서 각각 만족해도 매칭됨 (cross-object matching 문제)

## API 엔드포인트

### 관리 API
| Method | Endpoint                 |
|--------|--------------------------|
| PUT    | `/api/es/lnb/reindex`    | 전체 재인덱싱 (백그라운드)
| PUT    | `/api/es/lnb/forcemerge` | 세그먼트 병합
| GET    | `/api/es/lnb/count`      | 인덱스 문서 수
| DELETE | `/api/es/lnb`            | 인덱스 삭제

### 집계 API
| Method | Endpoint               |
|--------|------------------------|
| GET    | `/api/es/lnb/agg/sd`   | 시도별 집계
| GET    | `/api/es/lnb/agg/sgg`  | 시군구별 집계
| GET    | `/api/es/lnb/agg/emd`  | 읍면동별 집계
| GET    | `/api/es/lnb/agg/grid` | 그리드 집계

## 인덱싱 방식
LC와 동일한 EMD 단위 병렬 처리 방식 사용.

| Config     | Value     |
|------------|-----------|
| Worker     | 10        | EMD 라운드로빈 분배
| Fetch Size | 1,000     | JPA Stream 힌트 (LnbIndexingService.STREAM_SIZE)
| Bulk Size  | 1,000     | ES bulk 단위 (LnbIndexingService.BATCH_SIZE)
| Dispatcher | IO        | 코루틴 디스패처

### 처리 순서
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:3, replica:0, buildings nested 매핑)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회 (N건)
    ├─ trade 조회 (최신 1건)
    ├─ LnbDocument 생성
    └─ ES bulk indexing
           ↓
[4] Forcemerge
           ↓
[5] 결과 반환
```

## 비즈니스 필터 (LcAggFilter)

### Building (nested 쿼리로 적용)
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

### Trade (object 필터)
| Param                           | Type       |
|---------------------------------|------------|
| tradeProperty                   | List       | 구분 (토지/건물)
| tradeContractDateStart/End      | LocalDate  | 거래일 범위
| tradeEffectiveAmountMin/Max     | Long       | 거래가 범위
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물면적당단가 범위
| tradeLandAmountPerM2Min/Max     | BigDecimal | 토지면적당단가 범위

## 인덱싱 성능 (Benchmark)

### 환경
| Item      | Value          |
|-----------|----------------|
| Worker    | 20             |
| Bulk Size | 3,000          |
| ES heap   | 4GB (xms=xmx)  |
| App heap  | 4GB            |

### 결과
| Item        | Value                        |
|-------------|------------------------------|
| Docs        | 39,668,370                   |
| Elapsed     | 2,134,148ms / 2,134s / 35.6m |


## 관련 파일
- Controller: `controller/rest/LnbRestController.kt`, `controller/rest/LnbAggRestController.kt`
- Web: `controller/web/LnbAggWebController.kt`
- Indexing: `es/service/LnbIndexingService.kt`
- Aggregation: `es/service/LnbAggregationService.kt`
- Grid Agg: `es/service/LnbGridAggregationService.kt`
- Document: `es/document/land/LnbDocument.kt`
- Template: `templates/es/lnb/agg.ftl`, `templates/es/lnb/grid.ftl`
