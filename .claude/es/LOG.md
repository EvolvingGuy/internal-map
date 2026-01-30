# ES Log Format

인덱싱 및 포스머지 로그 양식 정의.

---

## 1. Indexing Log

### 1.1 Single Index (lbt_1x16_fm, lbt_1x16_nofm)

```
[{TAG}] ========== Indexing Request (async, single index, {shards} shards) ==========
[{TAG}] ========== Indexing Start (single index, {shards} shards) ==========
[{TAG}] Total parcels: {totalCount}, expected bulks: {expectedBulks}
[{TAG}] EMD {emdCount} → {workerCount} workers
[{TAG}] Worker-{n} start: EMD {count} assigned
[{TAG}] Worker-{n} bulk #{bulkCount}/{expected}: {processed}/{total} ({%}) EMD={emd} ({emdIdx}/{emdTotal}), buildings {bldgCount}, trades {tradeCount}, step {stepMs}ms, acc {accSec}s
[{TAG}] Worker-{n} done: {indexed} docs, buildings {bldg}, trades {trade}, bulks {bulkCount}, {elapsed}
[{TAG}] ========== Indexing Complete ==========
[{TAG}] Total docs: {totalDocs}, buildings: {totalBldg}, trades: {totalTrades}, bulks: {bulkCount}, elapsed: {totalTime}
```

### 1.2 Uniform Partition (lbt_2x3_fm, lbt_2x3_nofm)

```
[{TAG}] ========== Indexing Request (async, uniform {partitionCount} partitions) ==========
[{TAG}] ========== Indexing Start ({partitionCount} partitions, PNU hash uniform) ==========
[{TAG}] Total parcels: {totalCount}, expected bulks: {expectedBulks}
[{TAG}] EMD {emdCount} → {workerCount} workers
[{TAG}] Worker-{n} start: EMD {count} assigned
[{TAG}] Worker-{n} bulk #{bulkCount}/{expected}: {processed}/{total} ({%}) EMD={emd} ({emdIdx}/{emdTotal}) → P1:{p1} P2:{p2}, buildings {bldgCount}, trades {tradeCount}, step {stepMs}ms, acc {accSec}s
[{TAG}] Worker-{n} done: {indexed} docs, buildings {bldg}, trades {trade}, bulks {bulkCount}, {elapsed}
[{TAG}] ========== Indexing Complete ==========
[{TAG}] Total docs: {totalDocs}, buildings: {totalBldg}, trades: {totalTrades}, bulks: {bulkCount}, elapsed: {totalTime}
[{TAG}] Partition docs: P1={p1Count}, P2={p2Count}
```

### 1.3 Region Partition (lbt_17x4_region_fm, lbt_17x4_region_nofm)

```
[{TAG}] ========== Indexing Request (async, region {partitionCount} indices) ==========
[{TAG}] ========== Indexing Start ({partitionCount} region indices, {shardsPerIndex} shards each) ==========
[{TAG}] Total parcels: {totalCount}, expected bulks: {expectedBulks}
[{TAG}] EMD {emdCount} → {workerCount} workers
[{TAG}] Worker-{n} start: EMD {count} assigned
[{TAG}] Worker-{n} bulk #{bulkCount}/{expected}: {processed}/{total} ({%}) EMD={emd} ({emdIdx}/{emdTotal}) → SD={sdCode}, buildings {bldgCount}, trades {tradeCount}, step {stepMs}ms, acc {accSec}s
[{TAG}] Worker-{n} done: {indexed} docs, buildings {bldg}, trades {trade}, bulks {bulkCount}, {elapsed}
[{TAG}] ========== Indexing Complete ==========
[{TAG}] Total docs: {totalDocs}, buildings: {totalBldg}, trades: {totalTrades}, bulks: {bulkCount}, elapsed: {totalTime}
[{TAG}] Region docs: 11={count}, 26={count}, 27={count}, 28={count}, 29={count}, 30={count}, 31={count}, 36={count}, 41={count}, 42={count}, 43={count}, 44={count}, 45={count}, 46={count}, 47={count}, 48={count}, 50={count}
```

---

## 2. Tag Mapping

| Index | Tag (log prefix) |
|-------|-----------------|
| lbt_1x16_fm | `LBT_1x16_FM` |
| lbt_1x16_nofm | `LBT_1x16_NOFM` |
| lbt_2x3_fm | `LBT_2x3_FM` |
| lbt_2x3_nofm | `LBT_2x3_NOFM` |
| lbt_17x4_region_fm | `LBT_17x4_REGION_FM` |
| lbt_17x4_region_nofm | `LBT_17x4_REGION_NOFM` |

---

## 3. Forcemerge Log (fm only)

### 3.1 Single Index

```
[{TAG}] ========== Forcemerge Start (1 index) ==========
[{TAG}] Forcemerge complete [{indexName}]: {elapsed}ms ({sec}s)
[{TAG}] ========== Forcemerge Complete ==========
```

### 3.2 Multi Index (uniform / region)

```
[{TAG}] ========== Forcemerge Start ({indexCount} indices) ==========
[{TAG}] Forcemerge complete [{indexName_1}]: {elapsed}ms ({sec}s)
[{TAG}] Forcemerge complete [{indexName_2}]: {elapsed}ms ({sec}s)
...
[{TAG}] Forcemerge complete [{indexName_N}]: {elapsed}ms ({sec}s)
[{TAG}] ========== Forcemerge Complete ==========
```

---

## 4. Timing Stats (per bulk)

각 벌크마다 아래 항목의 평균(avg)과 누적(acc)을 측정.

| Metric | Description |
|--------|-------------|
| summaries query | BuildingLedgerOutlineSummaries 조회 |
| outlines query | BuildingLedgerOutline 조회 |
| trades query | RealEstateTrade 조회 |
| doc creation | Document 객체 생성 |
| bulk indexing | OpenSearch bulk API 호출 |
| step total | 벌크 1회 전체 소요 |

---

## 5. REST Response Format

### Reindex Response (single index)

```json
{
  "action": "reindex",
  "status": "started",
  "async": true,
  "index": "{indexName}",
  "shards": {shardCount}
}
```

### Reindex Response (multi index)

```json
{
  "action": "reindex",
  "status": "started",
  "async": true,
  "indexPattern": "{pattern}_*",
  "partitions": {partitionCount},
  "shardsPerIndex": {shardCount}
}
```

### Forcemerge Response

```json
{
  "action": "forcemerge",
  "status": "started",
  "async": true,
  "indices": ["{index_1}", "{index_2}", "..."],
  "max_num_segments": 1
}
```

### Count Response (single)

```json
{
  "{indexName}": {count}
}
```

### Count Response (multi)

```json
{
  "{index_1}": {count1},
  "{index_2}": {count2}
}
```
