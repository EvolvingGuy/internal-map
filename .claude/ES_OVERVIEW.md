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
| /page/es/markers           | Markers - Type1 vs Type2, (Land->Reg) vs (Reg->Land) | O      | land_compact + registration               | 등기 필터가 있는 경우 등기 우선 조회가 강력
| /page/es/markers-geo       | Markers Geo - Geometry Object vs String              | O      | land_compact_geo + land_compact           | 토지 폴리곤 인덱싱 비용
| /page/es/markers-compare   | Markers Compare - Center vs Intersect                | O      | land_compact_geo + land_compact_intersect | 폴리곤 포함 vs 교차


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
필지 단위 인덱스. 비즈니스 필터 적용 가능. geometry를 String으로 저장.

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
| land.geometry                           | text      | GeoJSON 문자열 (렌더링용)
| building.mgmBldrgstPk                   | keyword   | 건축물대장키
| building.mainPurpsCdNm                  | keyword   | 주용도
| building.useAprDay                      | date      | 준공일
| building.totArea                        | double    | 연면적
| lastRealEstateTrade.property            | keyword   | 구분 (토지/건물)
| lastRealEstateTrade.contractDate        | date      | 거래연월
| lastRealEstateTrade.effectiveAmount     | long      | 거래가
| lastRealEstateTrade.buildingAmountPerM2 | double    | 건물면적당단가
| lastRealEstateTrade.landAmountPerM2     | double    | 토지면적당단가

### land_compact_geo (LC Geo)
land_compact와 동일하나 geometry를 Object로 저장 (이스케이프 오버헤드 제거)

| Field         | Type   |
|---------------|--------|
| land.geometry | object | GeoJSON Object (렌더링용)
| (others)      | -      | land_compact와 동일

### land_compact_intersect (LC Intersect)
geometry를 geo_shape로 저장하여 intersects 쿼리 지원

| Field         | Type      |
|---------------|-----------|
| land.geometry | geo_shape | GeoJSON (공간 쿼리용)
| (others)      | -         | land_compact와 동일

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
- 실거래는 애매

## FE 민트맵 연동
