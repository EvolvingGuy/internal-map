# 단일 쿼리 레이턴시 비교 (Worst-Case)

동일 데이터(87,723,475 Lucene docs)를 다른 인덱스/샤드 구조로 분배했을 때
**전국 범위 집계 쿼리의 단일 요청 레이턴시**를 비교한다. 최악 케이스(전국 bbox)의 하한을 측정.

인덱스 구조, 인덱싱/forcemerge 소요시간, 환경 스펙은 [INDEX.md](../benchmark/INDEX.md) 참조.

## 벤치마크 결과

> `f` = forcemerge, `nf` = no forcemerge, bbox = 전국 고정

### sd (시도)

| Case           | avg | p50 | p90 | p95 | p99 | stddev |
|----------------|-----|-----|-----|-----|-----|--------|
| 1/16 local f   |     |     |     |     |     |        |
| 1/16 local nf  |     |     |     |     |     |        |
| 1/16 cluster f |     |     |     |     |     |        |
| 4/4 local f    |     |     |     |     |     |        |
| 4/4 local nf   |     |     |     |     |     |        |
| 4/4 cluster f  |     |     |     |     |     |        |
| 2/3 local f    |     |     |     |     |     |        |
| 2/3 local nf   |     |     |     |     |     |        |
| 2/3 cluster f  |     |     |     |     |     |        |
| 17/2 local f   |     |     |     |     |     |        |
| 17/2 local nf  |     |     |     |     |     |        |
| 17/2 cluster f |     |     |     |     |     |        |

### sgg (시군구)

| Case           | avg | p50 | p90 | p95 | p99 | stddev |
|----------------|-----|-----|-----|-----|-----|--------|
| 1/16 local f   |     |     |     |     |     |        |
| 1/16 local nf  |     |     |     |     |     |        |
| 1/16 cluster f |     |     |     |     |     |        |
| 4/4 local f    |     |     |     |     |     |        |
| 4/4 local nf   |     |     |     |     |     |        |
| 4/4 cluster f  |     |     |     |     |     |        |
| 2/3 local f    |     |     |     |     |     |        |
| 2/3 local nf   |     |     |     |     |     |        |
| 2/3 cluster f  |     |     |     |     |     |        |
| 17/2 local f   |     |     |     |     |     |        |
| 17/2 local nf  |     |     |     |     |     |        |
| 17/2 cluster f |     |     |     |     |     |        |

### emd (읍면동)

| Case           | avg | p50 | p90 | p95 | p99 | stddev |
|----------------|-----|-----|-----|-----|-----|--------|
| 1/16 local f   |     |     |     |     |     |        |
| 1/16 local nf  |     |     |     |     |     |        |
| 1/16 cluster f |     |     |     |     |     |        |
| 4/4 local f    |     |     |     |     |     |        |
| 4/4 local nf   |     |     |     |     |     |        |
| 4/4 cluster f  |     |     |     |     |     |        |
| 2/3 local f    |     |     |     |     |     |        |
| 2/3 local nf   |     |     |     |     |     |        |
| 2/3 cluster f  |     |     |     |     |     |        |
| 17/2 local f   |     |     |     |     |     |        |
| 17/2 local nf  |     |     |     |     |     |        |
| 17/2 cluster f |     |     |     |     |     |        |

## 테스트 조건

- 벤치마크 스크립트: `VS_FORCEMERGE_BENCH.py`
- 2,000회 순차 실행, `request_cache=false` (OS page cache warm)
- profile OFF
- 로컬 Docker (단일 노드, 14 cores, 6GB heap)

### Query 구성

**bbox**: 전국 고정 (worst-case). 쿼리마다 ±0.05도 jitter 적용하여 node query cache 회피.

```
top_left:     { lat: 38.8 ± 0.05, lon: 124.5 ± 0.05 }
bottom_right: { lat: 33.0 ± 0.05, lon: 132.0 ± 0.05 }
```

**agg level**: sd / sgg / emd round-robin 균등 배분 (각 ~667회). size 제한 없이 전체 반환.

| Level | Field | Size    |
|-------|-------|---------|
| sd    | sd    | 100     | 시도 (~17건)
| sgg   | sgg   | 10,000  | 시군구 (~250건)
| emd   | emd   | 100,000 | 읍면동 (~3,500건)

각 agg에 `geo_centroid` 서브 집계 포함 (지도 마커 좌표 계산).

**filter**: 토지 / 건물(nested) / 실거래(nested) 3개 카테고리에서 랜덤 조합. 최소 1개 카테고리, 각 카테고리 내 필터도 확률적으로 on/off.

| Category | Probability | Filters |
|----------|-------------|---------|
| 토지     | 60%         | 용도지역(terms), 지목(terms), 면적(range), 공시지가(range) |
| 건물     | 50%         | 주용도(terms), 등기구분(terms), 연면적(range), 사용승인일(range), 허가일 최근5년(bool), 착공일 최근5년(bool) |
| 실거래   | 50%         | 매물유형(terms), 계약일(range), 실거래가(range), 건물 단가/m²(range) |

### Sample Payloads

**예시 1**: 토지 필터만 (용도지역 + 면적), emd 집계

```json
{
  "size": 0,
  "query": {
    "bool": {
      "must": [{ "geo_bounding_box": { "land.center": {
        "top_left": { "lat": 38.82, "lon": 124.48 },
        "bottom_right": { "lat": 33.03, "lon": 131.97 }
      }}}],
      "filter": [
        { "terms": { "land.jiyukCd1": ["64", "71"] }},
        { "range": { "land.area": { "gte": 200, "lte": 3000 }}}
      ]
    }
  },
  "aggs": { "by_region": {
    "terms": { "field": "emd", "size": 100000 },
    "aggs": { "center": { "geo_centroid": { "field": "land.center" }}}
  }}
}
```

**예시 2**: 건물(nested) + 실거래(nested) 복합 필터, sgg 집계

```json
{
  "size": 0,
  "query": {
    "bool": {
      "must": [{ "geo_bounding_box": { "land.center": {
        "top_left": { "lat": 38.76, "lon": 124.53 },
        "bottom_right": { "lat": 32.98, "lon": 132.04 }
      }}}],
      "filter": [
        { "nested": { "path": "buildings", "query": { "bool": { "filter": [
          { "terms": { "buildings.mainPurpsCdNm": ["공동주택", "제1종근린생활시설"] }},
          { "range": { "buildings.totArea": { "gte": 50, "lte": 500 }}}
        ]}}}},
        { "nested": { "path": "trades", "query": { "bool": { "filter": [
          { "terms": { "trades.property": ["APARTMENT", "OFFICETEL"] }},
          { "range": { "trades.contractDate": { "gte": "2022-01-01", "lte": "2024-06-30" }}}
        ]}}}}
      ]
    }
  },
  "aggs": { "by_region": {
    "terms": { "field": "sgg", "size": 10000 },
    "aggs": { "center": { "geo_centroid": { "field": "land.center" }}}
  }}
}
```

**예시 3**: 토지 + 건물 + 실거래 전체 필터, sd 집계

```json
{
  "size": 0,
  "query": {
    "bool": {
      "must": [{ "geo_bounding_box": { "land.center": {
        "top_left": { "lat": 38.84, "lon": 124.46 },
        "bottom_right": { "lat": 33.02, "lon": 132.01 }
      }}}],
      "filter": [
        { "terms": { "land.jimokCd": ["02", "08"] }},
        { "range": { "land.price": { "gte": 50000, "lte": 1500000 }}},
        { "nested": { "path": "buildings", "query": { "bool": { "filter": [
          { "terms": { "buildings.regstrGbCdNm": ["집합"] }},
          { "range": { "buildings.useAprDay": { "gte": "2000-01-01", "lte": "2020-12-31" }}}
        ]}}}},
        { "nested": { "path": "trades", "query": { "bool": { "filter": [
          { "terms": { "trades.property": ["LAND", "SINGLE"] }},
          { "range": { "trades.effectiveAmount": { "gte": 50000000, "lte": 500000000 }}}
        ]}}}}
      ]
    }
  },
  "aggs": { "by_region": {
    "terms": { "field": "sd", "size": 100 },
    "aggs": { "center": { "geo_centroid": { "field": "land.center" }}}
  }}
}
```
