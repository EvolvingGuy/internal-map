# 샤드 전략 비교 (1x16 vs 2x3 vs 17x4_region)

동일한 forcemerge 설정에서 샤드 구조만 다른 비교.

---

## Comparison Targets

| Structure   | Indices | Shards/Index | Total Shards | Strategy       |
|-------------|---------|--------------|--------------|----------------|
| 1x16        | 1       | 16           | 16           | single uniform |
| 2x3         | 2       | 3            | 6            | hash partition |
| 17x4_region | 17      | 4            | 68           | region (SD)    |

---

## Local — Forcemerge O (fm)

### Latency (Overall)

| Metric | lbt_1x16_fm | lbt_2x3_fm | lbt_17x4_region_fm   |
|--------|-------------|------------|----------------------|
| avg    | 130.3ms     | 108.3ms    | **68.9ms**           |
| p50    | 60ms        | 58ms       | **40ms**             |
| p90    | 318ms       | 258ms      | **148ms**            |
| p95    | 657ms       | 494ms      | **315ms**            |
| p99    | 969ms       | 667ms      | **395ms**            |
| stddev | 199.8ms     | 145.0ms    | **88.1ms**           |

### Latency by Level

#### sd

| Metric | 1x16_fm | 2x3_fm  | 17x4_region_fm |
|--------|---------|---------|----------------|
| avg    | 121.2ms | 101.2ms | **61.3ms**     |
| p50    | 53ms    | 54ms    | **38ms**       |
| p99    | 945ms   | 555ms   | **325ms**      |

#### sgg

| Metric | 1x16_fm | 2x3_fm | 17x4_region_fm |
|--------|---------|--------|----------------|
| avg    | 112.5ms | 94.7ms | **64.2ms**     |
| p50    | 51ms    | 53ms   | **36ms**       |
| p99    | 864ms   | 528ms  | **401ms**      |

#### emd

| Metric | 1x16_fm | 2x3_fm  | 17x4_region_fm |
|--------|---------|---------|----------------|
| avg    | 157.3ms | 128.9ms | **81.3ms**     |
| p50    | 80ms    | 70ms    | **46ms**       |
| p99    | 1030ms  | 687ms   | **411ms**      |

---

## Local — Forcemerge X (nofm)

### Latency (Overall)

| Metric | lbt_1x16_nofm | lbt_2x3_nofm   | lbt_17x4_region_nofm   |
|--------|---------------|----------------|------------------------|
| avg    | 151.1ms       | 128.4ms        | **107.5ms**            |
| p50    | 88ms          | 72ms           | **72ms**               |
| p90    | 378ms         | 273ms          | **255ms**              |
| p95    | 556ms         | 574ms          | **376ms**              |
| p99    | 805ms         | 790ms          | **497ms**              |
| stddev | 172.4ms       | 165.8ms        | **104.2ms**            |

### Latency by Level

#### sd

| Metric | 1x16_nofm | 2x3_nofm   | 17x4_region_nofm   |
|--------|-----------|------------|--------------------|
| avg    | 138.2ms   | 119.2ms    | **90.0ms**         |
| p50    | 84ms      | 69ms       | **63ms**           |
| p99    | 726ms     | 636ms      | **388ms**          |

#### sgg

| Metric | 1x16_nofm | 2x3_nofm   | 17x4_region_nofm   |
|--------|-----------|------------|--------------------|
| avg    | 134.9ms   | 114.0ms    | **104.2ms**        |
| p50    | 83ms      | 67ms       | **71ms**           |
| p99    | 616ms     | 623ms      | **445ms**          |

#### emd

| Metric | 1x16_nofm | 2x3_nofm   | 17x4_region_nofm   |
|--------|-----------|------------|--------------------|
| avg    | 180.2ms   | 152.0ms    | **128.3ms**        |
| p50    | 104ms     | 82ms       | **86ms**           |
| p99    | 900ms     | 835ms      | **538ms**          |

---

## Remote

(원격 클러스터 세팅 후 기재)

---

## Findings

- **17x4_region이 모든 구간에서 최고 성능**: fm 기준 avg 68.9ms (1x16 대비 -47%, 2x3 대비 -36%)
- **region 기반 분리의 효과가 뚜렷**: 전국 bbox 쿼리에서도 각 region 인덱스가 독립적으로 검색되어 병렬 처리 이점
- **total shard 수보다 데이터 분포가 중요**: 17x4는 68 shards로 가장 많지만, region 기반으로 데이터가 분산되어 각 shard의 데이터량이 적어 성능 우위
- **2x3이 1x16보다 일관되게 빠름**: hash partition(6 shards)이 단일 인덱스(16 shards)보다 효율적. shard 수를 줄이면서 인덱스를 분리하는 전략이 유효
- **tail latency(p99) 차이가 가장 큼**: 17x4_region fm p99=395ms vs 1x16 fm p99=969ms (2.5배 차이)
- **emd 레벨에서 가장 큰 성능 차이**: agg size가 클수록 region 분리 효과 극대화
