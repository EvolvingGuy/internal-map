# Indexing Results

---

## Dataset

| Metric          | Value          |
|-----------------|----------------|
| Total Documents | 39,668,369     |
| Total Buildings | 6,210,982      |
| Total Trades    | 41,844,124     |

---

## Local (Docker single-node)

fm/nofm 쌍은 한쪽을 인덱싱 후 Docker volume 복사로 생성.

### Per Index

#### lbt_1x16_fm

| Metric                 | Value                  |
|------------------------|------------------------|
| Total Documents        | 39,668,369             |
| Total Buildings        | 6,210,982              |
| Total Trades           | 41,844,124             |
| Bulk Requests          | 42,338                 |
| Indexing Time          | 24.57m (1473.9s)       |
| Forcemerge Time        | 미기록                 |
| Segments/Shard (after) | 1                      |
| Note                   | 원본 인덱싱, nofm은 볼륨 복사 |

#### lbt_1x16_nofm

| Metric                 | Value                  |
|------------------------|------------------------|
| Total Documents        | 39,668,369             |
| Total Buildings        | 6,210,982              |
| Total Trades           | 41,844,124             |
| Bulk Requests          | -                      |
| Indexing Time          | (볼륨 복사)            |
| Forcemerge Time        | N/A                    |
| Segments/Shard (after) | (natural)              |
| Note                   | fm 볼륨 복사 + alias   |

#### lbt_2x3_fm

| Metric                 | Value                      |
|------------------------|----------------------------|
| Total Documents        | 39,668,369                 |
| Total Buildings        | 6,210,982                  |
| Total Trades           | 41,844,124                 |
| Bulk Requests          | -                          |
| Indexing Time          | (볼륨 복사)                |
| Forcemerge Time        | 미기록                     |
| Segments/Shard (after) | 1                          |
| Partition Distribution | P1=19,835,179 / P2=19,833,190 |
| Note                   | nofm 볼륨 복사 + alias     |

#### lbt_2x3_nofm

| Metric                 | Value                      |
|------------------------|----------------------------|
| Total Documents        | 39,668,369                 |
| Total Buildings        | 6,210,982                  |
| Total Trades           | 41,844,124                 |
| Bulk Requests          | 42,338                     |
| Indexing Time          | 23.81m (1428.4s)           |
| Forcemerge Time        | N/A                        |
| Segments/Shard (after) | (natural)                  |
| Partition Distribution | P1=19,835,179 / P2=19,833,190 |
| Note                   | 원본 인덱싱               |

#### lbt_17x4_region_fm

| Metric                 | Value                      |
|------------------------|----------------------------|
| Total Documents        | 39,668,369                 |
| Total Buildings        | 6,210,982                  |
| Total Trades           | 41,844,124                 |
| Bulk Requests          | -                          |
| Indexing Time          | (볼륨 복사)                |
| Forcemerge Time        | 미기록                     |
| Segments/Shard (after) | 1                          |
| Note                   | nofm 볼륨 복사 + 51/52 _reindex 보정 |

#### lbt_17x4_region_nofm

| Metric                 | Value                      |
|------------------------|----------------------------|
| Total Documents        | 39,668,369                 |
| Total Buildings        | 6,210,982                  |
| Total Trades           | 41,844,124                 |
| Bulk Requests          | 42,338                     |
| Indexing Time          | 21.90m (1314.2s)           |
| Forcemerge Time        | N/A                        |
| Segments/Shard (after) | (natural)                  |
| Note                   | 원본 인덱싱 + 51/52 _reindex 보정 |

### Region Distribution (17x4, fm/nofm 동일)

| SD | Region    | Documents   |
|----|-----------|-------------|
| 11 | Seoul     | 903,166     |
| 26 | Busan     | 714,165     |
| 27 | Daegu     | 791,389     |
| 28 | Incheon   | 669,158     |
| 29 | Gwangju   | 390,256     |
| 30 | Daejeon   | 292,049     |
| 31 | Ulsan     | 507,628     |
| 36 | Sejong    | 206,484     |
| 41 | Gyeonggi  | 5,164,227   |
| 43 | Chungbuk  | 2,395,086   |
| 44 | Chungnam  | 3,740,187   |
| 46 | Jeonnam   | 5,899,035   |
| 47 | Gyeongbuk | 5,694,523   |
| 48 | Gyeongnam | 4,819,948   |
| 50 | Jeju      | 880,280     |
| 51 | Gangwon   | 2,729,990   |
| 52 | Jeonbuk   | 3,870,798   |

### Summary (Local)

| Tag                   | Indices | Total Shards | Strategy | Indexing Time    | Forcemerge Time |
|-----------------------|---------|--------------|----------|------------------|-----------------|
| lbt_1x16_fm           | 1       | 16           | uniform  | 24.57m           | 미기록          |
| lbt_1x16_nofm         | 1       | 16           | uniform  | (볼륨 복사)      | N/A             |
| lbt_2x3_fm            | 2       | 6            | hash     | (볼륨 복사)      | 미기록          |
| lbt_2x3_nofm          | 2       | 6            | hash     | 23.81m           | N/A             |
| lbt_17x4_region_fm    | 17      | 68           | region   | (볼륨 복사)      | 미기록          |
| lbt_17x4_region_nofm  | 17      | 68           | region   | 21.90m           | N/A             |

---

## Remote (TBD)

(원격 클러스터 세팅 후 기재)
