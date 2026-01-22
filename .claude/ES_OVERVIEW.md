# ES (Elasticsearch) Overview

---

## Pages

| Page                       | Description                                              |
|----------------------------|----------------------------------------------------------|
| /page/es/lsrc/sd           | LSRC SD - Static region cluster, Sido                    | 클러스터) 필터X) 정적 시도 (행정구역마다 모든 정보 사전 합산)
| /page/es/lsrc/sgg          | LSRC SGG - Static region cluster, Sigungu                | 클러스터) 필터X) 정적 시군구
| /page/es/lsrc/emd          | LSRC EMD - Static region cluster, Eupmyeondong           | 클러스터) 필터X) 정적 읍면동
| /page/es/ldrc/sd           | LDRC SD - Dynamic region cluster, Sido                   | 클러스터) 필터X) 동적 시도 (H3 기반, ES aggregation)
| /page/es/ldrc/sgg          | LDRC SGG - Dynamic region cluster, Sigungu               | 클러스터) 필터X) 동적 시군구 (H3 기반, ES aggregation)
| /page/es/ldrc/emd          | LDRC EMD - Dynamic region cluster, Eupmyeondong          | 클러스터) 필터X) 동적 읍면동 (H3 기반, ES aggregation)
| /page/es/lc/agg/sd         | LC Agg SD - Aggregation with filters                     | 클러스터) 필터O) 동적 시도
| /page/es/lc/agg/sgg        | LC Agg SGG - Aggregation with filters                    | 클러스터) 필터O) 동적 시군구
| /page/es/lc/agg/emd        | LC Agg EMD - Aggregation with filters                    | 클러스터) 필터O) 동적 읍면동
| /page/es/lc/agg/grid       | LC Agg Grid - Grid-style clustering                      | 클러스터) 필터O) 동적 그리드
| /page/es/markers           | Markers - Type1 vs Type2, (Land -> Reg) vs (Reg -> Land) | 마커) 토지를 먼저 찾느냐 vs 등기를 먼저 찾느냐 등기 필터 설정 시 체감 큼
| /page/es/markers-geo       | Markers Geo - Type1 vs Type2, Geometry vs String         | 마커) 용량 차이는 미미함 (ES 인덱싱이라는 오버헤드가 있으므로 단순 렌더링 용이면 String이 낮다는 의미)
| /page/es/markers-compare   | Markers Compare - Center vs Intersect                    | 마커) 포함 vs 교차
