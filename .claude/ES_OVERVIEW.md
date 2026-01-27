# ES (Elasticsearch) Overview

---

## Pages

| Page                       | Description                                          | Filter | ES Index                                  |
|----------------------------|------------------------------------------------------|--------|-------------------------------------------|
| /page/es/lsrc/sd           | LSRC SD - Static region cluster, Sido                | X      | land_static_region_cluster                | 행정구역 정적 클러스터
| /page/es/lsrc/sgg          | LSRC SGG - Static region cluster, Sigungu            | X      | land_static_region_cluster                |
| /page/es/lsrc/emd          | LSRC EMD - Static region cluster, Eupmyeondong       | X      | land_static_region_cluster                |
| /page/es/ldrc/sd           | LDRC SD - Dynamic region cluster, Sido               | X      | land_dynamic_region_cluster               | 행정구역 동적 클러스터
| /page/es/ldrc/sgg          | LDRC SGG - Dynamic region cluster, Sigungu           | X      | land_dynamic_region_cluster               |
| /page/es/ldrc/emd          | LDRC EMD - Dynamic region cluster, Eupmyeondong      | X      | land_dynamic_region_cluster               |
| /page/es/lc/agg/sd         | LC Agg SD - Aggregation with filters                 | O      | land_compact                              | 행정구역 동적 클러스터
| /page/es/lc/agg/sgg        | LC Agg SGG - Aggregation with filters                | O      | land_compact                              |
| /page/es/lc/agg/emd        | LC Agg EMD - Aggregation with filters                | O      | land_compact                              |
| /page/es/lc/agg/grid       | LC Agg Grid - Grid-style clustering                  | O      | land_compact                              | 그리드 (무필터 버전 추가 가능)
| /page/es/lnb/agg/sd        | LNB Agg SD - Nested building filter                  | O      | land_nested_building                      | 건물 nested array
| /page/es/lnb/agg/sgg       | LNB Agg SGG - Nested building filter                 | O      | land_nested_building                      |
| /page/es/lnb/agg/emd       | LNB Agg EMD - Nested building filter                 | O      | land_nested_building                      |
| /page/es/lnb/agg/grid      | LNB Agg Grid - Nested building filter                | O      | land_nested_building                      |
| /page/es/lnbt/agg/sd       | LNBT Agg SD - Nested building+trade filter           | O      | land_nested_building_trade                | 건물+실거래 nested array
| /page/es/lnbt/agg/sgg      | LNBT Agg SGG - Nested building+trade filter          | O      | land_nested_building_trade                |
| /page/es/lnbt/agg/emd      | LNBT Agg EMD - Nested building+trade filter          | O      | land_nested_building_trade                |
| /page/es/lnbt/agg/grid     | LNBT Agg Grid - Nested building+trade filter         | O      | land_nested_building_trade                |
| /page/es/lcp/agg/sd        | LCP Agg SD - Point version of LC                     | O      | land_compact_point                        | geo_point only (경량)
| /page/es/lcp/agg/sgg       | LCP Agg SGG - Point version of LC                    | O      | land_compact_point                        |
| /page/es/lcp/agg/emd       | LCP Agg EMD - Point version of LC                    | O      | land_compact_point                        |
| /page/es/lnbp/agg/sd       | LNBP Agg SD - Point version of LNB                   | O      | land_nested_building_point                | geo_point only (경량)
| /page/es/lnbp/agg/sgg      | LNBP Agg SGG - Point version of LNB                  | O      | land_nested_building_point                |
| /page/es/lnbp/agg/emd      | LNBP Agg EMD - Point version of LNB                  | O      | land_nested_building_point                |
| /page/es/lnbtp/agg/sd      | LNBTP Agg SD - Point version of LNBT                 | O      | land_nested_building_trade_point          | geo_point only (경량)
| /page/es/lnbtp/agg/sgg     | LNBTP Agg SGG - Point version of LNBT                | O      | land_nested_building_trade_point          |
| /page/es/lnbtp/agg/emd     | LNBTP Agg EMD - Point version of LNBT                | O      | land_nested_building_trade_point          |

## APIs

| API                          | Method | Page                         |
|------------------------------|--------|------------------------------|
| /api/es/lsrc/query/sd        | GET    | /page/es/lsrc/sd             | 시도 고정 클러스터 조회
| /api/es/lsrc/query/sgg       | GET    | /page/es/lsrc/sgg            | 시군구 고정 클러스터 조회
| /api/es/lsrc/query/emd       | GET    | /page/es/lsrc/emd            | 읍면동 고정 클러스터 조회
| /api/es/ldrc/clusters        | GET    | /page/es/ldrc/*              | 동적 행정구역 클러스터 조회
| /api/es/lc/agg/sd            | GET    | /page/es/lc/agg/sd           | 시도 집계 (필터)
| /api/es/lc/agg/sgg           | GET    | /page/es/lc/agg/sgg          | 시군구 집계 (필터)
| /api/es/lc/agg/emd           | GET    | /page/es/lc/agg/emd          | 읍면동 집계 (필터)
| /api/es/lc/agg/grid          | GET    | /page/es/lc/agg/grid         | 그리드 클러스터링
| /api/es/lnb/agg/sd           | GET    | /page/es/lnb/agg/sd          | 시도 집계 (nested 건물 필터)
| /api/es/lnb/agg/sgg          | GET    | /page/es/lnb/agg/sgg         | 시군구 집계 (nested 건물 필터)
| /api/es/lnb/agg/emd          | GET    | /page/es/lnb/agg/emd         | 읍면동 집계 (nested 건물 필터)
| /api/es/lnb/agg/grid         | GET    | /page/es/lnb/agg/grid        | 그리드 클러스터링 (nested 건물 필터)
| /api/es/lnbt/agg/sd          | GET    | /page/es/lnbt/agg/sd         | 시도 집계 (nested 건물+실거래 필터)
| /api/es/lnbt/agg/sgg         | GET    | /page/es/lnbt/agg/sgg        | 시군구 집계 (nested 건물+실거래 필터)
| /api/es/lnbt/agg/emd         | GET    | /page/es/lnbt/agg/emd        | 읍면동 집계 (nested 건물+실거래 필터)
| /api/es/lnbt/agg/grid        | GET    | /page/es/lnbt/agg/grid       | 그리드 클러스터링 (nested 건물+실거래 필터)
| /api/es/lcp/agg/sd           | GET    | /page/es/lcp/agg/sd          | 시도 집계 (geo_bounding_box)
| /api/es/lcp/agg/sgg          | GET    | /page/es/lcp/agg/sgg         | 시군구 집계 (geo_bounding_box)
| /api/es/lcp/agg/emd          | GET    | /page/es/lcp/agg/emd         | 읍면동 집계 (geo_bounding_box)
| /api/es/lnbp/agg/sd          | GET    | /page/es/lnbp/agg/sd         | 시도 집계 (geo_bounding_box, nested 건물)
| /api/es/lnbp/agg/sgg         | GET    | /page/es/lnbp/agg/sgg        | 시군구 집계 (geo_bounding_box, nested 건물)
| /api/es/lnbp/agg/emd         | GET    | /page/es/lnbp/agg/emd        | 읍면동 집계 (geo_bounding_box, nested 건물)
| /api/es/lnbtp/agg/sd         | GET    | /page/es/lnbtp/agg/sd        | 시도 집계 (geo_bounding_box, nested 건물+실거래)
| /api/es/lnbtp/agg/sgg        | GET    | /page/es/lnbtp/agg/sgg       | 시군구 집계 (geo_bounding_box, nested 건물+실거래)
| /api/es/lnbtp/agg/emd        | GET    | /page/es/lnbtp/agg/emd       | 읍면동 집계 (geo_bounding_box, nested 건물+실거래)

---

## ES Index Models

### land_static_region_cluster (LSRC)
고정형 행정구역 클러스터. 뷰포트 무관하게 행정구역 전체 필지 카운트.

| Field   | Type      |
|---------|-----------|
| id      | keyword   | {level}_{code}
| level   | keyword   | SD, SGG, EMD
| code    | keyword   | 행정구역 코드
| name    | keyword   | 행정구역 이름
| count   | integer   | 필지 수
| center  | geo_point | 중심 좌표

### land_dynamic_region_cluster (LDRC)
동적 행정구역 클러스터. H3 기반으로 뷰포트에 걸친 영역만 집계.

| Field      | Type    |
|------------|---------|
| id         | keyword | {level}_{h3Index}_{code}
| level      | keyword | SD, SGG, EMD
| h3Index    | long    | H3 인덱스
| regionCode | long    | 행정구역 코드
| cnt        | integer | 필지 수
| sumLat     | double  | 위도 합 (중심점 계산용)
| sumLng     | double  | 경도 합 (중심점 계산용)

### land_compact (LC)
필지 단위 인덱스. 비즈니스 필터 적용 가능. geometry를 geo_shape로 저장하여 intersects 쿼리 지원.

| Field                                   | Type      |
|-----------------------------------------|-----------|
| pnu                                     | keyword   | PNU 코드 19자리
| sd                                      | keyword   | 시도 코드 2자리
| sgg                                     | keyword   | 시군구 코드 5자리
| emd                                     | keyword   | 읍면동 코드 8자리
| land.jiyukCd1                           | keyword   | 용도지역 코드
| land.jimokCd                            | keyword   | 지목 코드
| land.area                               | double    | 토지면적
| land.price                              | long      | 개별공시지가
| land.center                             | geo_point | 중심 좌표
| land.geometry                           | geo_shape | GeoJSON (intersects 쿼리용)
| building.mgmBldrgstPk                   | keyword   | 건축물대장키
| building.mainPurpsCdNm                  | keyword   | 주용도
| building.useAprDay                      | date      | 준공일
| building.totArea                        | double    | 연면적
| lastRealEstateTrade.property            | keyword   | 구분 (토지/건물)
| lastRealEstateTrade.contractDate        | date      | 거래연월
| lastRealEstateTrade.effectiveAmount     | long      | 거래가
| lastRealEstateTrade.buildingAmountPerM2 | double    | 건물면적당단가
| lastRealEstateTrade.landAmountPerM2     | double    | 토지면적당단가

### land_nested_building (LNB)
필지 단위 인덱스. **건물을 nested array로 저장**하여 개별 건물 단위 필터링 지원.
LC와 동일 구조이나 building → buildings (nested array)로 변경.

| Field                                   | Type      |
|-----------------------------------------|-----------|
| pnu                                     | keyword   | PNU 코드 19자리
| sd                                      | keyword   | 시도 코드 2자리
| sgg                                     | keyword   | 시군구 코드 5자리
| emd                                     | keyword   | 읍면동 코드 8자리
| land.jiyukCd1                           | keyword   | 용도지역 코드
| land.jimokCd                            | keyword   | 지목 코드
| land.area                               | double    | 토지면적
| land.price                              | long      | 개별공시지가
| land.center                             | geo_point | 중심 좌표
| land.geometry                           | geo_shape | GeoJSON (intersects 쿼리용)
| **buildings**                           | **nested**| 건물 배열 (1:N)
| buildings.mgmBldrgstPk                  | keyword   | 건축물대장키
| buildings.mainPurpsCdNm                 | keyword   | 주용도
| buildings.regstrGbCdNm                  | keyword   | 등기구분
| buildings.pmsDay                        | date      | 허가일
| buildings.stcnsDay                      | date      | 착공일
| buildings.useAprDay                     | date      | 준공일
| buildings.totArea                       | double    | 연면적
| buildings.platArea                      | double    | 대지면적
| buildings.archArea                      | double    | 건축면적
| lastRealEstateTrade.property            | keyword   | 구분 (토지/건물)
| lastRealEstateTrade.contractDate        | date      | 거래연월
| lastRealEstateTrade.effectiveAmount     | long      | 거래가
| lastRealEstateTrade.buildingAmountPerM2 | double    | 건물면적당단가
| lastRealEstateTrade.landAmountPerM2     | double    | 토지면적당단가

**nested 쿼리 특징:**
- 개별 건물 단위로 정확한 필터링 가능
- 예: "준공일 2020년 이후 건물이 있는 필지" 정확 매칭
- LC의 object 타입은 cross-object matching 문제 있음

### land_nested_building_trade (LNBT)
필지 단위 인덱스. **건물과 실거래 모두 nested array로 저장**하여 개별 단위 필터링 지원.
LNB와 동일 구조이나 lastRealEstateTrade → trades (nested array)로 변경.

| Field                                   | Type      |
|-----------------------------------------|-----------|
| pnu                                     | keyword   | PNU 코드 19자리
| sd                                      | keyword   | 시도 코드 2자리
| sgg                                     | keyword   | 시군구 코드 5자리
| emd                                     | keyword   | 읍면동 코드 8자리
| land.jiyukCd1                           | keyword   | 용도지역 코드
| land.jimokCd                            | keyword   | 지목 코드
| land.area                               | double    | 토지면적
| land.price                              | long      | 개별공시지가
| land.center                             | geo_point | 중심 좌표
| land.geometry                           | geo_shape | GeoJSON (intersects 쿼리용)
| **buildings**                           | **nested**| 건물 배열 (1:N)
| buildings.mgmBldrgstPk                  | keyword   | 건축물대장키
| buildings.mainPurpsCdNm                 | keyword   | 주용도
| buildings.regstrGbCdNm                  | keyword   | 등기구분
| buildings.pmsDay                        | date      | 허가일
| buildings.stcnsDay                      | date      | 착공일
| buildings.useAprDay                     | date      | 준공일
| buildings.totArea                       | double    | 연면적
| buildings.platArea                      | double    | 대지면적
| buildings.archArea                      | double    | 건축면적
| **trades**                              | **nested**| 실거래 배열 (1:N)
| trades.property                         | keyword   | 구분 (토지/건물)
| trades.contractDate                     | date      | 거래연월
| trades.effectiveAmount                  | long      | 거래가
| trades.buildingAmountPerM2              | double    | 건물면적당단가
| trades.landAmountPerM2                  | double    | 토지면적당단가

**nested 쿼리 특징:**
- 개별 건물 + 개별 실거래 단위로 정확한 필터링 가능
- 예: "2023년 이후 거래된 필지 중 5억 이상" 정확 매칭
- LNB의 lastRealEstateTrade(1건)과 달리 전체 거래 이력(N건) 저장

### land_compact_point (LCP)
LC의 경량 버전. **geometry 없이 geo_point만 저장**하여 용량 최소화.

| Feature       | LC                    | LCP                   |
|---------------|-----------------------|-----------------------|
| 공간 데이터    | geometry (geo_shape)  | 없음                   |
| 공간 쿼리      | geo_shape intersects  | geo_bounding_box      |
| 인덱스 용량    | ~19GB                 | ~12GB (37% 감소)      |

### land_nested_building_point (LNBP)
LNB의 경량 버전. **geometry 없이 geo_point만 저장**하여 용량 최소화.
건물은 nested array 유지.

| Feature       | LNB                   | LNBP                  |
|---------------|-----------------------|-----------------------|
| 공간 데이터    | geometry (geo_shape)  | 없음                   |
| 공간 쿼리      | geo_shape intersects  | geo_bounding_box      |
| 건물 저장      | nested array          | nested array          |

### land_nested_building_trade_point (LNBTP)
LNBT의 경량 버전. **geometry 없이 geo_point만 저장**하여 용량 최소화.
건물과 실거래 모두 nested array 유지.

| Feature       | LNBT                  | LNBTP                 |
|---------------|-----------------------|-----------------------|
| 공간 데이터    | geometry (geo_shape)  | 없음                   |
| 공간 쿼리      | geo_shape intersects  | geo_bounding_box      |
| 건물 저장      | nested array          | nested array          |
| 실거래 저장    | nested array          | nested array          |

### registration
등기 데이터 인덱스. markers에서 land_compact와 join.

| Field               | Type      |
|---------------------|-----------|
| id                  | integer   | 등기 ID
| pnuId               | keyword   | PNU 코드
| realEstateNumber    | keyword   | 부동산고유번호
| registrationType    | keyword   | 등기 유형
| registrationProcess | keyword   | 등기 상태
| property            | keyword   | 구분 (토지/건물)
| completedAt         | date      | 완료일
| createdAt           | date      | 생성일
| userId              | long      | 사용자 ID
| center              | geo_point | 중심 좌표
| geometry            | geo_shape | 폴리곤 (공간 쿼리용)

---

## ES 인덱스 모델링
- 현재 실거래는 마지막 건으로 한정되어 합쳐진 상태
- 등기같은 공적장부는 자주 발행하니 모델에서 분리하는 게 더 좋지 않을까. 물론 그것도 적은 편이지만
- 토지 & 건축물 & 실거래는
- 토지 및 건축물의 변동성 주기가 낮으니 같이 ES 인덱싱
- 실거래와 등기가 고민
- 등기는 확실히 계속 바뀌고
- 실거래는 토지에 비해서는 더 자주 바뀔 수 있는 형태고

## FE 민트맵 연동

---

## API Request/Response

### 공통 BBox 파라미터
모든 API는 아래 bbox 파라미터 필수:

| Param | Type   | Required |
|-------|--------|----------|
| swLng | Double | O        | 남서 경도
| swLat | Double | O        | 남서 위도
| neLng | Double | O        | 북동 경도
| neLat | Double | O        | 북동 위도

---

### /api/es/lsrc/query/{level}
고정형 행정구역 클러스터 조회 (level: sd, sgg, emd)

**Request:** BBox만

**Response: LsrcQueryResult**
```
{
  regions: [
    { code: String, name: String, cnt: Int, centerLat: Double, centerLng: Double }
  ],
  totalCount: Int,
  elapsedMs: Long
}
```

---

### /api/es/ldrc/clusters
동적 행정구역 클러스터 조회 (H3 기반)

**Request:**
| Param | Type   | Required | Default |
|-------|--------|----------|---------|
| level | String | X        | SD      | SD, SGG, EMD
| zoom  | Int    | X        | 10      | 네이버맵 줌레벨

**Response: LdrcResponse**
```
{
  level: String,
  h3Count: Int,
  clusters: [
    { code: Long, name: String, count: Long, centerLat: Double, centerLng: Double }
  ],
  totalCount: Long,
  elapsedMs: Long
}
```

---

### /api/es/lc/agg/{level}
필터 적용 행정구역 집계 (level: sd, sgg, emd)

**Request:** BBox + LcAggFilter

**Response: LcAggResponse**
```
{
  level: String,
  totalCount: Long,
  regionCount: Int,
  regions: [
    { code: String, name: String?, count: Long, centerLat: Double, centerLng: Double }
  ],
  elapsedMs: Long
}
```

---

### /api/es/lc/agg/grid
그리드 클러스터링

**Request:** BBox + LcAggFilter +
| Param          | Type | Required | Default |
|----------------|------|----------|---------|
| viewportWidth  | Int  | O        | -       | 뷰포트 너비 (px)
| viewportHeight | Int  | O        | -       | 뷰포트 높이 (px)
| gridSize       | Int  | X        | 400     | 그리드 셀 크기 (px)

**Response: LcGridAggResponse**
```
{
  cols: Int,
  rows: Int,
  totalCount: Long,
  cellCount: Int,
  cells: [
    { gridX: Int, gridY: Int, count: Long, centerLat: Double, centerLng: Double }
  ],
  elapsedMs: Long
}
```

---

### LcAggFilter (공통 필터)

**Building 필터:**
| Param                    | Type          |
|--------------------------|---------------|
| buildingMainPurpsCdNm    | String (CSV)  | 주용도
| buildingRegstrGbCdNm     | String (CSV)  | 등기구분
| buildingPmsDayRecent5y   | Boolean       | 최근5년 허가
| buildingStcnsDayRecent5y | Boolean       | 최근5년 착공
| buildingUseAprDayStart   | Int           | 준공년도 시작
| buildingUseAprDayEnd     | Int           | 준공년도 종료
| buildingTotAreaMin/Max   | BigDecimal    | 연면적 범위
| buildingPlatAreaMin/Max  | BigDecimal    | 대지면적 범위
| buildingArchAreaMin/Max  | BigDecimal    | 건축면적 범위

**Land 필터:**
| Param             | Type         |
|-------------------|--------------|
| landJiyukCd1      | String (CSV) | 용도지역
| landJimokCd       | String (CSV) | 지목
| landAreaMin/Max   | Double       | 토지면적 범위
| landPriceMin/Max  | Long         | 공시지가 범위

**Trade 필터:**
| Param                        | Type         |
|------------------------------|--------------|
| tradeProperty                | String (CSV) | 구분 (토지/건물)
| tradeContractDateStart/End   | LocalDate    | 거래일 범위
| tradeEffectiveAmountMin/Max  | Long         | 거래가 범위
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물면적당단가 범위
| tradeLandAmountPerM2Min/Max  | BigDecimal   | 토지면적당단가 범위

---

## ES Commands

### Forcemerge

**App API:**
```bash
# geo_shape 인덱스
curl -X PUT http://localhost:3000/api/es/lc/forcemerge
curl -X PUT http://localhost:3000/api/es/lnb/forcemerge
curl -X PUT http://localhost:3000/api/es/lnbt/forcemerge

# geo_point 인덱스 (경량)
curl -X PUT http://localhost:3000/api/es/lcp/forcemerge
curl -X PUT http://localhost:3000/api/es/lnbp/forcemerge
curl -X PUT http://localhost:3000/api/es/lnbtp/forcemerge

# 클러스터
curl -X PUT http://localhost:3000/api/es/lsrc/forcemerge
curl -X PUT http://localhost:3000/api/es/ldrc/forcemerge
```

**ES Direct:**
```bash
curl -X POST "http://localhost:9200/{index}/_forcemerge?max_num_segments=1"
```

### Task Monitoring

```bash
# forcemerge 태스크 확인
curl -s "http://localhost:9200/_tasks?actions=*forcemerge*"

# 모든 태스크 확인
curl -s "http://localhost:9200/_cat/tasks?v"
```

### Segment Status

```bash
# 세그먼트 상태 확인
curl -s "http://localhost:9200/_cat/segments/{index}?v"

# 인덱스 상태 확인
curl -s "http://localhost:9200/_cat/indices?v"
```

### Reindex

**App API:**
```bash
# geo_shape 인덱스
curl -X PUT http://localhost:3000/api/es/lc/reindex
curl -X PUT http://localhost:3000/api/es/lnb/reindex
curl -X PUT http://localhost:3000/api/es/lnbt/reindex

# geo_point 인덱스 (경량)
curl -X PUT http://localhost:3000/api/es/lcp/reindex
curl -X PUT http://localhost:3000/api/es/lnbp/reindex
curl -X PUT http://localhost:3000/api/es/lnbtp/reindex

# 클러스터
curl -X PUT http://localhost:3000/api/es/lsrc/reindex
curl -X PUT http://localhost:3000/api/es/ldrc/reindex
```

### Index Management

```bash
# 인덱스 삭제
curl -X DELETE http://localhost:3000/api/es/lc
curl -X DELETE http://localhost:3000/api/es/lnb
curl -X DELETE http://localhost:3000/api/es/lnbt
curl -X DELETE http://localhost:3000/api/es/lcp
curl -X DELETE http://localhost:3000/api/es/lnbp
curl -X DELETE http://localhost:3000/api/es/lnbtp

# 문서 수 확인
curl http://localhost:3000/api/es/lc/count
curl http://localhost:3000/api/es/lnb/count
curl http://localhost:3000/api/es/lnbt/count
curl http://localhost:3000/api/es/lcp/count
curl http://localhost:3000/api/es/lnbp/count
curl http://localhost:3000/api/es/lnbtp/count
```