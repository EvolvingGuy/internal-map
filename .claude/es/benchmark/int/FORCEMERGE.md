# Forcemerge (INT)

## Environment

| Item          | Value                                  |
|---------------|----------------------------------------|
| OpenSearch    | 2.11.1 (AWS OpenSearch Service)        |
| Instance      | r6g.large (2 vCPU, 16GB)              |
| Nodes         | 3                                      |

## Forcemerge Results

`max_num_segments=1` 적용. ForcemergeHelper 비동기 실행 + 세그먼트 수렴 폴링으로 소요시간 측정.

### lbt_17x4_region_fm

| Item            | Value |
|-----------------|-------|
| Total Shards    | 68    |
| Forcemerge Time |       |
| Segments Before |       |
| Segments After  | 68 (1 per shard) |

### lbt_4x4_fm

| Item            | Value |
|-----------------|-------|
| Total Shards    | 16    |
| Forcemerge Time |       |
| Segments Before |       |
| Segments After  | 16 (1 per shard) |

### lbt_2x3_fm

| Item            | Value |
|-----------------|-------|
| Total Shards    | 6     |
| Forcemerge Time |       |
| Segments Before |       |
| Segments After  | 6 (1 per shard) |
