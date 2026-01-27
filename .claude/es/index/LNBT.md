# LNBT (Land Nested Building Trade)
필지 단위 인덱스 - 건물과 실거래 모두 nested array로 저장하여 개별 단위 필터링 지원

## 인덱스 정보
| key        | value                         |
|------------|-------------------------------|
| IndexName  | `land_nested_building_trade`  |
| Shards     | 3                             |
| Replicas   | 0                             |

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
    "geometry": { "type": "Polygon", "coordinates": [...] }
  },
  "buildings": [
    { "mgmBldrgstPk": "11110-100012345", "mainPurpsCdNm": "단독주택", "useAprDay": "2020-05-15", "totArea": 150.5 },
    { "mgmBldrgstPk": "11110-100012346", "mainPurpsCdNm": "제2종근린생활시설", "useAprDay": "2022-03-10", "totArea": 85.2 }
  ],
  "trades": [
    { "property": "LAND", "contractDate": "2024-01-15", "effectiveAmount": 500000000, "landAmountPerM2": 1512000 },
    { "property": "LAND", "contractDate": "2023-06-20", "effectiveAmount": 480000000, "landAmountPerM2": 1451000 },
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

### nested 쿼리의 장점 (실거래)
- **정확한 필터링**: "2023년 이후 거래 + 5억 이상 거래가 있는 필지" 정확 매칭
- LNB의 lastRealEstateTrade는 최신 1건만 저장하여 과거 거래 필터링 불가
- **전체 거래 이력 저장**: 하나의 필지에 여러 거래가 있을 경우 모두 저장

## API 엔드포인트

### 관리 API
| Method | Endpoint                  |
|--------|---------------------------|
| PUT    | `/api/es/lnbt/reindex`    | 전체 재인덱싱 (백그라운드)
| PUT    | `/api/es/lnbt/forcemerge` | 세그먼트 병합
| GET    | `/api/es/lnbt/count`      | 인덱스 문서 수
| DELETE | `/api/es/lnbt`            | 인덱스 삭제

### 집계 API
| Method | Endpoint                |
|--------|-------------------------|
| GET    | `/api/es/lnbt/agg/sd`   | 시도별 집계
| GET    | `/api/es/lnbt/agg/sgg`  | 시군구별 집계
| GET    | `/api/es/lnbt/agg/emd`  | 읍면동별 집계
| GET    | `/api/es/lnbt/agg/grid` | 그리드 집계

## 인덱싱 방식
EMD (읍면동) 단위 병렬 처리 방식 사용. 실거래 조회는 N건.

| Config     | Value     |
|------------|-----------|
| Worker     | 10        | EMD 라운드로빈 분배
| Fetch Size | 1000      | JPA Stream 힌트 (LnbtIndexingService.STREAM_SIZE)
| Bulk Size  | 1000      | ES bulk 단위 (LnbtIndexingService.BATCH_SIZE)
| Dispatcher | IO        | 코루틴 디스패처

### 처리 순서
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:3, replica:0, buildings+trades nested 매핑)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회 (N건)
    ├─ trade 조회 (N건) ← LNB와 차이점
    ├─ LnbtDocument 생성
    └─ ES bulk indexing
           ↓
[4] Forcemerge
           ↓
[5] 결과 반환
```

### 실거래 조회 쿼리
```kotlin
// LNB: 최신 1건만
realEstateTradeRepo.findLatestByPnuIn(pnuCsv)

// LNBT: 전체 N건
realEstateTradeRepo.findAllByPnuIn(pnuCsv)
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

### Trade (nested 쿼리로 적용) ← LNB와 차이점
| Param                           | Type       |
|---------------------------------|------------|
| tradeProperty                   | List       | 구분 (토지/건물)
| tradeContractDateStart/End      | LocalDate  | 거래일 범위
| tradeEffectiveAmountMin/Max     | Long       | 거래가 범위
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물면적당단가 범위
| tradeLandAmountPerM2Min/Max     | BigDecimal | 토지면적당단가 범위

## Nested Array 통계

### 전체 통계
| Type | Total Nested Docs | 평균/필지 |
|------|-------------------|-----------|
| trades | 41,380,628 | ~1.04 |
| buildings | 6,210,908 | ~0.16 |

### trades 최대 필지 TOP 10
| PNU | trades 건수 |
|-----|------------|
| 1135010500106910000 | 9,998 |
| 1171010800101500000 | 9,975 |
| 1135010500106240000 | 9,781 |
| 4155025031100360001 | 9,760 |
| 1162010100117120000 | 9,703 |
| 4119210900103970000 | 9,687 |
| 1168011800105270000 | 9,575 |
| 4121010300106500000 | 9,459 |
| 4111312900111800000 | 9,303 |
| 1129010300106090001 | 9,302 |

### buildings 최대 필지 TOP 10
| PNU | buildings 건수 |
|-----|---------------|
| 4711110700100010001 | 285 |
| 4376025029105550000 | 282 |
| 4711110800100010365 | 211 |
| 4825012700114170000 | 190 |
| 4790037059100880000 | 184 |
| 4711110800100010367 | 176 |
| 2917011500101300001 | 161 |
| 4711111000100050005 | 161 |
| 5173033026102090000 | 160 |
| 2823710300101810001 | 140 |

### 요약
- 전체 문서 수: ~39,668,066개
- trades: 최대 ~10,000건/필지 (대형 상업시설/아파트 단지)
- buildings: 최대 ~285개/필지 (대규모 단지)
- 대부분 필지는 실거래 0~2건, 건물 0~1개 수준

## 관련 파일
- Controller: `controller/rest/LnbtRestController.kt`, `controller/rest/LnbtAggRestController.kt`
- Web: `controller/web/LnbtAggWebController.kt`
- Indexing: `es/service/LnbtIndexingService.kt`
- Aggregation: `es/service/LnbtAggregationService.kt`
- Grid Agg: `es/service/LnbtGridAggregationService.kt`
- Document: `es/document/land/LnbtDocument.kt`
- Repository: `jpa/repository/RealEstateTradeRepository.kt` (findAllByPnuIn 추가)
- Template: `templates/es/lnbt/agg.ftl`, `templates/es/lnbt/grid.ftl`
