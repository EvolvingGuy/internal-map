┌──────────┬──────────────────────────────┬─────────────────────────────┐
│   Type   │            Page              │            API              │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Table    │ /page/pnu/agg/emd_11         │ /api/pnu/agg/emd_11         │
│          │ /page/pnu/agg/emd_10         │ /api/pnu/agg/emd_10         │
│          │ /page/pnu/agg/emd_09         │ /api/pnu/agg/emd_09         │
│          │ /page/pnu/agg/sgg_08         │ /api/pnu/agg/sgg_08         │
│          │ /page/pnu/agg/sgg_07         │ /api/pnu/agg/sgg_07         │
│          │ /page/pnu/agg/sd_06          │ /api/pnu/agg/sd_06          │
│          │ /page/pnu/agg/sd_05          │ /api/pnu/agg/sd_05          │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Dynamic  │ /page/pnu/agg/dynamic/emd_11 │ /api/pnu/agg/dynamic/emd_11 │
│          │ /page/pnu/agg/dynamic/emd_10 │ /api/pnu/agg/dynamic/emd_10 │
│          │ /page/pnu/agg/dynamic/emd_09 │ /api/pnu/agg/dynamic/emd_09 │
│          │ /page/pnu/agg/dynamic/sgg_08 │ /api/pnu/agg/dynamic/sgg_08 │
│          │ /page/pnu/agg/dynamic/sgg_07 │ /api/pnu/agg/dynamic/sgg_07 │
│          │ /page/pnu/agg/dynamic/sd_06  │ /api/pnu/agg/dynamic/sd_06  │
│          │ /page/pnu/agg/dynamic/sd_05  │ /api/pnu/agg/dynamic/sd_05  │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Static   │ /page/pnu/agg/static/emd     │ /api/pnu/agg/static/emd     │
│          │ /page/pnu/agg/static/sgg     │ /api/pnu/agg/static/sgg     │
│          │ /page/pnu/agg/static/sd      │ /api/pnu/agg/static/sd      │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Grid     │ /page/pnu/agg/grid           │ /api/pnu/agg/grid           │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Count    │ /page/pnu/agg/count          │ /api/pnu/agg/count          │
├──────────┼──────────────────────────────┼─────────────────────────────┤
│ Indicator│ (모든 페이지에서 호출)        │ /api/pnu/agg/indicator      │
└──────────┴──────────────────────────────┴─────────────────────────────┘


## Table API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox (swLng, swLat, neLng, neLat)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: bbox -> H3 indexes                                          │
│                                                                     │
│   H3Util.bboxToH3Indexes(bbox, resolution)                          │
│   resolution = endpoint (emd_11=11, emd_10=10, ..., sd_05=5)        │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Redis cache multi-get                                       │
│                                                                     │
│   key = "pnu_agg_{table}:{h3Index}"                                 │
│   hit -> use cached data                                            │
│   miss -> go to STEP 3                                              │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: DB query for cache miss                                     │
│                                                                     │
│   SELECT * FROM r3_pnu_agg_{table}                                  │
│   WHERE h3_index IN (missing h3 indexes)                            │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Redis cache multi-set                                       │
│                                                                     │
│   cache DB result (including empty = negative cache)                │
│   TTL = 1 day                                                       │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: List<AggData>                                             │
│                                                                     │
│   { code, h3Index, cnt, sumLat, sumLng }                            │
│   centerLat = sumLat / cnt                                          │
│   centerLng = sumLng / cnt                                          │
└─────────────────────────────────────────────────────────────────────┘


## Dynamic API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox (swLng, swLat, neLng, neLat)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: bbox -> H3 indexes                                          │
│                                                                     │
│   H3Util.bboxToH3Indexes(bbox, resolution)                          │
│   resolution = endpoint (emd_11=11, emd_10=10, ..., sd_05=5)        │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Redis cache multi-get                                       │
│                                                                     │
│   key = "pnu_agg_{table}:{h3Index}"                                 │
│   (same cache layer as Table API)                                   │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: DB query for cache miss + cache set                         │
│                                                                     │
│   (same as Table API STEP 3-4)                                      │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Group by region code                                        │
│                                                                     │
│   aggregate by code (emd/sgg/sd)                                    │
│   sum(cnt), sum(sumLat), sum(sumLng)                                │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: List<DynamicData>                                         │
│                                                                     │
│   { code, cnt, centerLat, centerLng }                               │
│   centerLat = totalSumLat / totalCnt                                │
│   centerLng = totalSumLng / totalCnt                                │
└─────────────────────────────────────────────────────────────────────┘


## Static API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox + regionLevel (emd/sgg/sd)                            │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: regionLevel -> H3 resolution                                │
│                                                                     │
│   EMD -> res 9                                                      │
│   SGG -> res 7                                                      │
│   SD  -> res 5                                                      │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: bbox -> H3 indexes                                          │
│                                                                     │
│   H3Util.bboxToH3Indexes(bbox, resolution)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: Redis cache multi-get (static cache)                        │
│                                                                     │
│   key = "pnu_agg_static:{level}:{h3Index}"                          │
│   value = List<StaticRegionData>                                    │
└─────────────────────────────────────────────────────────────────────┘
│
│  고정형은 별도 캐시를 사용함. (Table/Dynamic과 다른 캐시 레이어)
│  각 H3 셀마다 해당 셀에 걸치는 행정구역 리스트를 캐시함.
│  하나의 행정구역이 여러 H3 셀에 걸쳐있으면 각 셀마다 중복 저장됨.
│  예: 서울시가 H3 셀 100개에 걸쳐있으면 100개 키에 서울시 데이터 저장.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: DB query for cache miss (2-step)                            │
│                                                                     │
│   4-1. r3_pnu_agg_{level}_{res} -> get region codes by h3           │
│   4-2. r3_pnu_agg_static_region -> get full region data by codes    │
└─────────────────────────────────────────────────────────────────────┘
│
│  캐시 미스 시 2단계 DB 조회:
│  4-1. 먼저 H3 인덱스로 어떤 행정구역 코드들이 있는지 조회.
│  4-2. 그 코드들로 고정형 테이블에서 전체 카운트와 중심좌표 조회.
│  고정형은 뷰포트와 관계없이 행정구역 전체 필지수를 보여줌.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: Redis cache multi-set + deduplicate                         │
│                                                                     │
│   cache by h3Index                                                  │
│   remove duplicate regions (same code across h3 cells)              │
└─────────────────────────────────────────────────────────────────────┘
│
│  캐시 저장은 H3 인덱스 단위로 함. (중복 저장 허용)
│  응답 시에는 같은 행정구역 코드가 여러 H3 셀에서 나와도 한 번만 반환.
│  캐시는 중복 저장 / 응답은 중복 제거.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: List<StaticData>                                          │
│                                                                     │
│   { code, name, cnt, centerLat, centerLng }                         │
│   (fixed total count per region)                                    │
└─────────────────────────────────────────────────────────────────────┘


## Grid API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox + zoomLevel + viewportWidth + viewportHeight          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: zoomLevel -> table/resolution                               │
│                                                                     │
│   zoom 18-22 -> emd_11 (res 11)                                     │
│   zoom 16-17 -> emd_10 (res 10)                                     │
│   zoom 13-15 -> sgg_08 (res 8)                                      │
│   zoom 10-12 -> sd_06  (res 6)                                      │
│   zoom 0-9   -> sd_05  (res 5)                                      │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: calculate grid                                              │
│                                                                     │
│   cols = viewportWidth / 450px                                      │
│   rows = viewportHeight / 450px                                     │
│   cellWidth = (neLng - swLng) / cols                                │
│   cellHeight = (neLat - swLat) / rows                               │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: bbox -> H3 indexes                                          │
│                                                                     │
│   H3Util.bboxToH3Indexes(bbox, resolution)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Redis cache get + DB query (same as Table API)              │
│                                                                     │
│   (shares cache layer with Table/Dynamic API)                       │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: assign data to grid cells                                   │
│                                                                     │
│   for each data:                                                    │
│     centerLat = sumLat / cnt                                        │
│     centerLng = sumLng / cnt                                        │
│     col = (centerLng - swLng) / cellWidth                           │
│     row = (neLat - centerLat) / cellHeight                          │
│     gridCell(row, col) += { cnt, sumLat, sumLng }                   │
└─────────────────────────────────────────────────────────────────────┘
│
│  각 H3 데이터는 cnt(필지수), sumLat, sumLng를 갖고 있음.
│  sumLat/cnt, sumLng/cnt로 평균 좌표(중심점)를 구함.
│  중심점이 왼쪽 (swLng)에서 얼마나 떨어졌는지 → cellWidth로 나눔 → col(열)
│  중심점이 위쪽 (neLat)에서 얼마나 떨어졌는지 → cellHeight로 나눔 → row(행)
│  해당 (row, col) 칸에 cnt, sumLat, sumLng를 누적.
│  여러 H3 데이터가 같은 칸에 들어가면 합산됨.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: GridResponse                                              │
│                                                                     │
│   { cells[], totalCount, maxCount, cols, rows }                     │
│   cell = { row, col, cnt, lat, lng }                                │
│   circle size = sqrt(cnt / maxCount) * (MAX - MIN) + MIN            │
└─────────────────────────────────────────────────────────────────────┘


## Count API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox + zoomLevel                                           │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: zoomLevel -> table/resolution                               │
│                                                                     │
│   zoom 16-22 -> emd_10 (res 10)                                     │
│   zoom 12-15 -> sgg_07 (res 7)                                      │
│   zoom 0-11  -> sd_05  (res 5)                                      │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: bbox -> H3 indexes                                          │
│                                                                     │
│   H3Util.bboxToH3Indexes(bbox, resolution)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: Redis cache get + DB query (same as Table API)              │
│                                                                     │
│   (shares cache layer with Table/Dynamic API)                       │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: sum all counts                                              │
│                                                                     │
│   totalCount = sum(all data.cnt)                                    │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: CountResponse                                             │
│                                                                     │
│   { count, elapsedMs }                                              │
└─────────────────────────────────────────────────────────────────────┘


## Indicator API Flow
┌─────────────────────────────────────────────────────────────────────┐
│ REQUEST: bbox (swLng, swLat, neLng, neLat)                          │
└─────────────────────────────────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: bbox -> center point                                        │
│                                                                     │
│   centerLat = (swLat + neLat) / 2                                   │
│   centerLng = (swLng + neLng) / 2                                   │
└─────────────────────────────────────────────────────────────────────┘
│
│  뷰포트의 정중앙 좌표를 계산함.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: center -> H3 index (res 10)                                 │
│                                                                     │
│   H3Util.latLngToH3(centerLat, centerLng, 10)                       │
└─────────────────────────────────────────────────────────────────────┘
│
│  중심점이 속한 H3 셀 하나만 구함. (res 10 고정)
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3: emd_10 cache get (single H3)                                │
│                                                                     │
│   key = "pnu_agg_emd_10:{h3Index}"                                  │
│   (shares cache layer with Table/Dynamic API)                       │
└─────────────────────────────────────────────────────────────────────┘
│
│  Table/Dynamic API와 캐시 공유.
│  해당 H3 셀에 있는 법정동 코드 리스트 조회.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: select closest region                                       │
│                                                                     │
│   for each region in H3 cell:                                       │
│     avgLat = sumLat / cnt                                           │
│     avgLng = sumLng / cnt                                           │
│     dist = (centerLat - avgLat)² + (centerLng - avgLng)²            │
│   select region with min distance                                   │
└─────────────────────────────────────────────────────────────────────┘
│
│  하나의 H3 셀에 여러 법정동이 걸쳐있을 수 있음.
│  뷰포트 중심점에 가장 가까운 법정동 하나 선택.
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5: boundary_region cache get                                   │
│                                                                     │
│   key = "boundary_region:{regionCode}"                              │
│   regionCode = 10자리 법정동 코드                                    │
│   value = { code, name, fullName, geom, ... }                       │
└─────────────────────────────────────────────────────────────────────┘
│
│  10자리 법정동 코드로 boundary_region 테이블/캐시 조회.
│  fullName에 계층적 행정구역명이 들어있음. (예: "서울특별시 중구 신당동")
│
▼
┌─────────────────────────────────────────────────────────────────────┐
│ RESPONSE: CenterIndicatorResponse                                   │
│                                                                     │
│   { indicator, regionCode, elapsedMs }                              │
│   indicator = fullName.replace(" ", " > ")                          │
│   예: "서울특별시 > 중구 > 신당동"                                   │
└─────────────────────────────────────────────────────────────────────┘


## UI: Indicator Balloon (모든 PNU 페이지 공통)

┌───────────────────────────────────────────────────────────────────────┐
│  위치: 화면 상단 중앙 (top: 50px, left: 50%)                           │
│  스타일: 검은색 반투명 배경, 흰색 텍스트, 둥근 모서리                    │
│  내용: "서울특별시 > 중구 > 신당동" (계층적 행정구역)                    │
│  업데이트: 지도 이동/줌 시 300ms 디바운스로 갱신                        │
│  구현: templates/pnu/common/indicator.ftl (Freemarker macro)          │
└───────────────────────────────────────────────────────────────────────┘