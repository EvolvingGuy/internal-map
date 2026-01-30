# LNBTP (Land Nested Building Trade Point) 인덱스 통계 쿼리

인덱스명: `land_nested_building_trade_point`

---

## 문서 수 확인 쿼리

### 1. 전체 Lucene 문서 수 (parent + nested 모두 포함)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_stats?pretty" | grep -A2 '"docs"'
```

**결과:**
```
"docs" : {
  "count" : 87,723,475,
  "deleted" : 0
}
```

---

### 2. Parent 문서 수 (토지 문서)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_count?pretty"
```

**결과:**
```json
{
  "count" : 39,668,369
}
```

---

### 3. Nested trades 문서 수 (실거래)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "nested_trades": {
        "nested": {"path": "trades"},
        "aggs": {
          "count": {"value_count": {"field": "trades.contractDate"}}
        }
      }
    }
  }'
```

**결과:**
```json
{
  "aggregations": {
    "nested_trades": {
      "doc_count": 41,844,124,
      "count": {"value": 41844124}
    }
  }
}
```

---

### 4. Nested buildings 문서 수 (건물)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "nested_buildings": {
        "nested": {"path": "buildings"},
        "aggs": {
          "count": {"value_count": {"field": "buildings.mgmBldrgstPk"}}
        }
      }
    }
  }'
```

**결과:**
```json
{
  "aggregations": {
    "nested_buildings": {
      "doc_count": 6,210,982,
      "count": {"value": 6210982}
    }
  }
}
```

---

### 5. 인덱스 용량

```bash
curl -s "localhost:9200/_cat/indices/land_nested_building_trade_point?v&h=index,docs.count,store.size"
```

**결과:**
```
index                            docs.count store.size
land_nested_building_trade_point   87723475      8.1gb
```

---

## 문서 수 요약

| Type             | Count      |
|------------------|------------|
| Total Lucene     | 87,723,475 | parent + nested
| Parent           | 39,668,369 | 필지
| Nested trades    | 41,844,124 | 실거래
| Nested buildings | 6,210,982  | 건물

**검증:** 39,668,369 + 41,844,124 + 6,210,982 = 87,723,475 ✓

---

## 필드별 디스크 사용량 확인

```bash
curl -s -X POST "localhost:9200/land_nested_building_trade_point/_disk_usage?run_expensive_tasks=true&pretty"
```

### trades 필드 용량

| Field                       | Size  | Bytes/Record |
|-----------------------------|-------|--------------|
| trades.property             | 46MB  | 1.1          |
| trades.contractDate         | 231MB | 5.5          |
| trades.effectiveAmount      | 265MB | 6.3          |
| trades.buildingAmountPerM2  | 277MB | 6.6          |
| trades.landAmountPerM2      | 119MB | 2.8          |
| **Total (doc_values+points)** | **938MB** | **22.4** |

### 전체 용량 breakdown

| Component                   | Size   |
|-----------------------------|--------|
| _source (raw JSON)          | 3.7GB  |
| trades (doc_values + points)| ~1GB   |
| buildings                   | ~300MB |
| land + geo_point            | ~1.3GB |
| _id, _seq_no (meta)         | ~1GB   |
| **Total**                   | **8.1GB** |

---

## 참고: nested 문서 특성

- nested 문서는 Lucene 레벨에서 별도 문서로 저장됨
- `_count` API는 parent 문서만 카운트
- `_stats` API의 docs.count는 nested 포함 전체 Lucene 문서 수
- nested 문서의 `_source`는 parent에만 저장됨 (중복 없음)
