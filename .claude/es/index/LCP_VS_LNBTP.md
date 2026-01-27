# LCP vs LNBTP 인덱스 비교

## 인덱스 개요

| Field      | LCP                | LNBTP                            |
|------------|--------------------|----------------------------------|
| index_name | land_compact_point | land_nested_building_trade_point |
| structure  | flat               | nested                           | 토지+대표건물1+최근실거래1 vs 토지+전체건물+전체실거래
| geo_type   | geo_point          | geo_point                        |
| shards     | 3                  | 4                                |

---

## 문서 수 비교

| Type             | LCP        | LNBTP      | Diff          |
|------------------|------------|------------|---------------|
| Parent           | 39,668,369 | 39,668,369 | -             | 토지 문서
| Nested trades    | -          | 41,844,124 | +41,844,124   | 실거래
| Nested buildings | -          | 6,210,982  | +6,210,982    | 건물
| **Total docs**   | 39,668,369 | 87,723,475 | +48,055,106   | Lucene 전체

---

## 용량 비교

| Metric     | LCP   | LNBTP | Diff   |
|------------|-------|-------|--------|
| store.size | 5.7GB | 8.1GB | +2.4GB |

### 저장 영역별 비교

| Component      | LCP    | LNBTP  | Diff    |
|----------------|--------|--------|---------|
| _source        | 2.7GB  | 3.7GB  | +1GB    | nested array JSON
| doc_values     | 1.4GB  | 2GB    | +0.6GB  |
| points         | 799MB  | 1.3GB  | +0.5GB  | geo_point + numeric
| inverted_index | 626MB  | 840MB  | +0.2GB  |
| **Total**      | 5.7GB  | 8.1GB  | +2.4GB  |

### 용량 증가 분석

| Data                       | Count      | Size   |
|----------------------------|------------|--------|
| trades (doc_values+points) | 41,844,124 | ~1GB   |
| trades (_source JSON)      | -          | ~1GB   |
| buildings (doc_values)     | 6,210,982  | ~300MB |
| buildings (_source JSON)   | -          | ~200MB |
| Total                      | -          | ~2.5GB |

실측 증가: **2.4GB** (예상과 거의 일치)

---

## 매핑 구조 비교

### LCP (flat 구조)
```
├── pnu (keyword)
├── sd, sgg, emd (keyword)
├── land
│   ├── center (geo_point)
│   ├── jiyukCd1, jimokCd (keyword)
│   ├── area (double), price (long)
├── building (object, single)
│   ├── mgmBldrgstPk, mainPurpsCdNm, regstrGbCdNm (keyword)
│   ├── pmsDay, stcnsDay, useAprDay (date)
│   └── totArea, platArea, archArea (scaled_float)
└── lastRealEstateTrade (object, single)
    ├── property (keyword)
    ├── contractDate (date)
    └── effectiveAmount (long), buildingAmountPerM2, landAmountPerM2 (scaled_float)
```

### LNBTP (nested 구조)
```
├── pnu (keyword)
├── sd, sgg, emd (keyword)
├── land
│   ├── center (geo_point)
│   ├── jiyukCd1, jimokCd (keyword)
│   ├── area (double), price (long)
├── buildings (nested, array) ← diff
│   ├── mgmBldrgstPk, mainPurpsCdNm, regstrGbCdNm (keyword)
│   ├── pmsDay, stcnsDay, useAprDay (date)
│   └── totArea, platArea, archArea (scaled_float)
└── trades (nested, array) ← diff
    ├── property (keyword)
    ├── contractDate (date)
    └── effectiveAmount (long), buildingAmountPerM2, landAmountPerM2 (scaled_float)
```

---

## 결론

- **문서 수**: LNBTP가 2.2배 많음 (nested 포함)
- **용량**: LNBTP가 2.4GB 더 큼 (42% 증가)
- **nested 4800만건 추가 → 용량 2.4GB 증가**
- Lucene의 열 기반 압축(doc_values)으로 nested 문서당 약 50 bytes 수준

---

## 용량 차이가 적은 이유

### 1. nested 문서는 `_source`를 별도 저장하지 않음
- nested 문서(trades, buildings)의 원본 JSON은 **parent 문서의 `_source`에 배열로 포함**
- 8700만 Lucene docs 중 `_source`는 **3900만개 parent만** 저장
- nested 4800만개는 `_source` 중복 저장 없음

### 2. doc_values 압축이 매우 효율적

Lucene의 열 기반 저장소(doc_values)는 다음 기법으로 압축:

| Technique           |
|---------------------|
| Delta encoding      | 정렬된 값들 간 차이만 저장
| Bit packing         | 값 범위에 필요한 최소 비트만 사용
| Run-length encoding | 연속 동일값 압축
| Block compression   | 유사한 값들을 블록 단위로 압축

### 3. 필드별 실제 용량 (trades 기준)

| Field                       | Size  | Bytes/Record |
|-----------------------------|-------|--------------|
| trades.property             | 46MB  | 1.1          |
| trades.contractDate         | 231MB | 5.5          |
| trades.effectiveAmount      | 265MB | 6.3          |
| trades.buildingAmountPerM2  | 277MB | 6.6          |
| trades.landAmountPerM2      | 119MB | 2.8          |
| **Total**                   | 938MB | 22.4         |

- 4천만건 trades의 doc_values + points = **약 1GB**
- 레코드당 **22 bytes** 수준

### 4. 데이터 타입 선택 효과

| Type           |
|----------------|
| `scaled_float` | double 대신 long으로 저장, 압축률 향상
| `keyword`      | ordinal encoding으로 중복값 압축
| `date`         | epoch millis로 저장, delta encoding 적용
| `long`         | 값 범위 기반 bit packing

### 5. 비교: Raw JSON vs Lucene 저장

```
Raw JSON:
4천만 trades × 평균 150 bytes = 6GB

Lucene:
doc_values: ~1GB
_source 내 trades JSON (압축): ~1GB
Total: ~2GB

Compression ratio: 3x
```

---

## 근거 쿼리

### 1. 인덱스 목록 및 용량

```bash
curl -s "localhost:9200/_cat/indices?v&h=index,docs.count,store.size"
```

### 2. Parent 문서 수 (토지)

```bash
# LCP
curl -s "localhost:9200/land_compact_point/_count?pretty"

# LNBTP
curl -s "localhost:9200/land_nested_building_trade_point/_count?pretty"
```

### 3. Nested trades 수 (LNBTP)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "nested_trades": {
        "nested": {"path": "trades"},
        "aggs": {"count": {"value_count": {"field": "trades.contractDate"}}}
      }
    }
  }'
```

### 4. Nested buildings 수 (LNBTP)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "nested_buildings": {
        "nested": {"path": "buildings"},
        "aggs": {"count": {"value_count": {"field": "buildings.mgmBldrgstPk"}}}
      }
    }
  }'
```

### 5. 전체 Lucene 문서 수 (nested 포함)

```bash
curl -s "localhost:9200/land_nested_building_trade_point/_stats?pretty" | grep -A2 '"docs"'
```

### 6. 매핑 확인

```bash
curl -s "localhost:9200/land_compact_point/_mapping?pretty"
curl -s "localhost:9200/land_nested_building_trade_point/_mapping?pretty"
```

### 7. 필드별 디스크 사용량

```bash
# LCP
curl -s -X POST "localhost:9200/land_compact_point/_disk_usage?run_expensive_tasks=true&pretty"

# LNBTP
curl -s -X POST "localhost:9200/land_nested_building_trade_point/_disk_usage?run_expensive_tasks=true&pretty"
```
