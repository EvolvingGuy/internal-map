# ES Index Specification

---

## 1. Naming Convention

```
{l|lb|lbt}_{index}x{shard}[_{strategy}]_{fm|nofm}[_{partition}]
```

| Segment    | Description                        | Example                         |
|------------|------------------------------------|---------------------------------|
| prefix     | `l` / `lb` / `lbt` (nested scope)  | `lbt` = land + building + trade |
| structure  | `{index count}x{shard count}`      | `4x4`, `1x16`, `17x4`           |
| strategy   | omit = uniform, explicit = special | `region`                        |
| forcemerge | `fm` / `nofm`                      | required                        |
| partition  | multi-index suffix                 | `_1`, `_2`, `_11`               |

---

## 2. Test Matrix

| # | Tag                    | Index Pattern            | Indices  | Shards/Index | Total Shards | Strategy               | Forcemerge |
|---|------------------------|--------------------------|----------|--------------|--------------|------------------------|------------|
| 1 | `lbt_1x16_fm`          | `lbt_1x16_fm`            | 1        | 16           | 16           | uniform (single)       | O          |
| 2 | `lbt_1x16_nofm`        | `lbt_1x16_nofm`          | 1        | 16           | 16           | uniform (single)       | X          |
| 3 | `lbt_2x3_fm`           | `lbt_2x3_fm_*`           | 2        | 3            | 6            | uniform (PNU hash % 2) | O          |
| 4 | `lbt_2x3_nofm`         | `lbt_2x3_nofm_*`         | 2        | 3            | 6            | uniform (PNU hash % 2) | X          |
| 5 | `lbt_17x4_region_fm`   | `lbt_17x4_region_fm_*`   | 17       | 4            | 68           | region (SD code)       | O          |
| 6 | `lbt_17x4_region_nofm` | `lbt_17x4_region_nofm_*` | 17       | 4            | 68           | region (SD code)       | X          |

---

## 3. Environment

### Docker (OpenSearch)

| Item | Spec |
|------|------|
| Host OS | macOS (Apple Silicon, 14 cores) |
| Docker CPU | 6 cores (limited) |
| Docker Memory | 16GB |
| JVM Heap | 6GB |
| OS Page Cache | ~10GB (Docker managed) |
| OpenSearch | 2.11.1 (single node) |
| Storage | SSD |
| Network | localhost:9200 |

### Common Index Settings

| Parameter | Value |
|-----------|-------|
| Workers | 10 |
| Bulk Size | 1,000 |
| JPA Batch Size | 1,000 |
| Replicas | 0 |
| Nested Objects Limit | 100,000 |
| Dispatcher | IO (Coroutine) |
| Execution | Async |
| Geo Type | geo_point (fixed) |

### Data Volume

| Data | Count |
|------|-------|
| Land (parcels) | 39,668,369 |
| Buildings | 6,210,982 |
| Trades | 41,844,124 |
| Lucene Documents | 87,723,475 |
| Expected Bulk Requests | 42,338 |

---

## 4. Lifecycle

```
[Create Index] → [Reindex (async)] → [Forcemerge (fm only)] → [Benchmark] → [Load Test]
```

### REST Endpoints (per index)

```
PUT    /api/es/{tag}/reindex       # async indexing start
PUT    /api/es/{tag}/forcemerge    # async forcemerge (fm only)
GET    /api/es/{tag}/count         # document count
DELETE /api/es/{tag}               # delete index
GET    /api/es/{tag}/agg/sd        # sido aggregation
GET    /api/es/{tag}/agg/sgg       # sigungu aggregation
GET    /api/es/{tag}/agg/emd       # eupmyeondong aggregation
```

### Web Pages (per index)

```
/page/es/{tag}/agg/sd              # map - sido level
/page/es/{tag}/agg/sgg             # map - sigungu level
/page/es/{tag}/agg/emd             # map - eupmyeondong level
```

---

## 5. Index #1 — lbt_1x16_fm

| Item | Value |
|------|-------|
| Tag | `lbt_1x16_fm` |
| Index Name | `lbt_1x16_fm` |
| Index Count | 1 |
| Shards | 16 |
| Replicas | 0 |
| Strategy | uniform (single index) |
| Forcemerge | O (max_num_segments=1) |
| Query Pattern | `lbt_1x16_fm` |
| Docker Volume | `geo_poc_os_lbt_1x16_fm` |

---

## 6. Index #2 — lbt_1x16_nofm

| Item | Value |
|------|-------|
| Tag | `lbt_1x16_nofm` |
| Index Name | `lbt_1x16_nofm` |
| Index Count | 1 |
| Shards | 16 |
| Replicas | 0 |
| Strategy | uniform (single index) |
| Forcemerge | X |
| Query Pattern | `lbt_1x16_nofm` |
| Docker Volume | `geo_poc_os_lbt_1x16_nofm` |

---

## 7. Index #3 — lbt_2x3_fm

| Item | Value |
|------|-------|
| Tag | `lbt_2x3_fm` |
| Index Pattern | `lbt_2x3_fm_*` |
| Index Count | 2 |
| Shards/Index | 3 |
| Total Shards | 6 |
| Replicas | 0 |
| Strategy | uniform (PNU hash % 2) |
| Forcemerge | O (max_num_segments=1) |
| Docker Volume | `geo_poc_os_lbt_2x3_fm` |

### Partition Indices

| Partition | Index Name | Expected Docs |
|-----------|-----------|--------------|
| P1 | `lbt_2x3_fm_1` | ~19.8M |
| P2 | `lbt_2x3_fm_2` | ~19.8M |

---

## 8. Index #4 — lbt_2x3_nofm

| Item | Value |
|------|-------|
| Tag | `lbt_2x3_nofm` |
| Index Pattern | `lbt_2x3_nofm_*` |
| Index Count | 2 |
| Shards/Index | 3 |
| Total Shards | 6 |
| Replicas | 0 |
| Strategy | uniform (PNU hash % 2) |
| Forcemerge | X |
| Docker Volume | `geo_poc_os_lbt_2x3_nofm` |

### Partition Indices

| Partition | Index Name | Expected Docs |
|-----------|-----------|--------------|
| P1 | `lbt_2x3_nofm_1` | ~19.8M |
| P2 | `lbt_2x3_nofm_2` | ~19.8M |

---

## 9. Index #5 — lbt_17x4_region_fm

| Item | Value |
|------|-------|
| Tag | `lbt_17x4_region_fm` |
| Index Pattern | `lbt_17x4_region_fm_*` |
| Index Count | 17 |
| Shards/Index | 4 |
| Total Shards | 68 |
| Replicas | 0 |
| Strategy | region (SD code, 17 regions) |
| Forcemerge | O (max_num_segments=1) |
| Docker Volume | `geo_poc_os_lbt_17x4_region_fm` |

### Region Indices

| SD | Region | Index Name |
|----|--------|-----------|
| 11 | Seoul | `lbt_17x4_region_fm_11` |
| 26 | Busan | `lbt_17x4_region_fm_26` |
| 27 | Daegu | `lbt_17x4_region_fm_27` |
| 28 | Incheon | `lbt_17x4_region_fm_28` |
| 29 | Gwangju | `lbt_17x4_region_fm_29` |
| 30 | Daejeon | `lbt_17x4_region_fm_30` |
| 31 | Ulsan | `lbt_17x4_region_fm_31` |
| 36 | Sejong | `lbt_17x4_region_fm_36` |
| 41 | Gyeonggi | `lbt_17x4_region_fm_41` |
| 42 | Gangwon | `lbt_17x4_region_fm_42` |
| 43 | Chungbuk | `lbt_17x4_region_fm_43` |
| 44 | Chungnam | `lbt_17x4_region_fm_44` |
| 45 | Jeonbuk | `lbt_17x4_region_fm_45` |
| 46 | Jeonnam | `lbt_17x4_region_fm_46` |
| 47 | Gyeongbuk | `lbt_17x4_region_fm_47` |
| 48 | Gyeongnam | `lbt_17x4_region_fm_48` |
| 50 | Jeju | `lbt_17x4_region_fm_50` |

---

## 10. Index #6 — lbt_17x4_region_nofm

| Item | Value |
|------|-------|
| Tag | `lbt_17x4_region_nofm` |
| Index Pattern | `lbt_17x4_region_nofm_*` |
| Index Count | 17 |
| Shards/Index | 4 |
| Total Shards | 68 |
| Replicas | 0 |
| Strategy | region (SD code, 17 regions) |
| Forcemerge | X |
| Docker Volume | `geo_poc_os_lbt_17x4_region_nofm` |

### Region Indices

| SD | Region | Index Name |
|----|--------|-----------|
| 11 | Seoul | `lbt_17x4_region_nofm_11` |
| 26 | Busan | `lbt_17x4_region_nofm_26` |
| 27 | Daegu | `lbt_17x4_region_nofm_27` |
| 28 | Incheon | `lbt_17x4_region_nofm_28` |
| 29 | Gwangju | `lbt_17x4_region_nofm_29` |
| 30 | Daejeon | `lbt_17x4_region_nofm_30` |
| 31 | Ulsan | `lbt_17x4_region_nofm_31` |
| 36 | Sejong | `lbt_17x4_region_nofm_36` |
| 41 | Gyeonggi | `lbt_17x4_region_nofm_41` |
| 42 | Gangwon | `lbt_17x4_region_nofm_42` |
| 43 | Chungbuk | `lbt_17x4_region_nofm_43` |
| 44 | Chungnam | `lbt_17x4_region_nofm_44` |
| 45 | Jeonbuk | `lbt_17x4_region_nofm_45` |
| 46 | Jeonnam | `lbt_17x4_region_nofm_46` |
| 47 | Gyeongbuk | `lbt_17x4_region_nofm_47` |
| 48 | Gyeongnam | `lbt_17x4_region_nofm_48` |
| 50 | Jeju | `lbt_17x4_region_nofm_50` |
