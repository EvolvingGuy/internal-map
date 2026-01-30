# Indexing Results

---

## Local (Docker single-node)

### Per Index

#### lbt_1x16_fm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | |
| Segments/Shard (after) | 1 |

#### lbt_1x16_nofm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | N/A |
| Segments/Shard (after) | (natural) |

#### lbt_2x3_fm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | |
| Segments/Shard (after) | 1 |
| Partition Distribution | P1= / P2= |

#### lbt_2x3_nofm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | N/A |
| Segments/Shard (after) | (natural) |
| Partition Distribution | P1= / P2= |

#### lbt_17x4_region_fm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | |
| Segments/Shard (after) | 1 |

Region Distribution:

| SD | Region | Documents |
|----|--------|-----------|
| 11 | Seoul | |
| 26 | Busan | |
| 27 | Daegu | |
| 28 | Incheon | |
| 29 | Gwangju | |
| 30 | Daejeon | |
| 31 | Ulsan | |
| 36 | Sejong | |
| 41 | Gyeonggi | |
| 42 | Gangwon | |
| 43 | Chungbuk | |
| 44 | Chungnam | |
| 45 | Jeonbuk | |
| 46 | Jeonnam | |
| 47 | Gyeongbuk | |
| 48 | Gyeongnam | |
| 50 | Jeju | |

#### lbt_17x4_region_nofm

| Metric | Value |
|--------|-------|
| Total Documents | |
| Total Buildings | |
| Total Trades | |
| Bulk Requests | |
| Indexing Time | |
| Forcemerge Time | N/A |
| Segments/Shard (after) | (natural) |

### Summary (Local)

| Tag | Indices | Total Shards | Strategy | Indexing Time | Forcemerge Time |
|-----|---------|-------------|----------|--------------|----------------|
| lbt_1x16_fm | 1 | 16 | uniform | | |
| lbt_1x16_nofm | 1 | 16 | uniform | | |
| lbt_2x3_fm | 2 | 6 | uniform | | |
| lbt_2x3_nofm | 2 | 6 | uniform | | |
| lbt_17x4_region_fm | 17 | 68 | region | | |
| lbt_17x4_region_nofm | 17 | 68 | region | | |

---

## Remote (TBD)

(원격 클러스터 세팅 후 기재)
