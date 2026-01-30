# 샤드 전략 비교 (1x16 vs 2x3 vs 17x4_region)

동일한 forcemerge 설정에서 샤드 구조만 다른 비교.

---

## Comparison Targets

| Structure | Indices | Shards/Index | Total Shards | Strategy |
|-----------|---------|-------------|-------------|----------|
| 1x16 | 1 | 16 | 16 | single uniform |
| 2x3 | 2 | 3 | 6 | hash partition |
| 17x4_region | 17 | 4 | 68 | region (SD) |

---

## Local — Forcemerge O (fm)

### Latency (Overall)

| Metric | lbt_1x16_fm | lbt_2x3_fm | lbt_17x4_region_fm |
|--------|-------------|------------|---------------------|
| avg | | | |
| p50 | | | |
| p90 | | | |
| p95 | | | |
| p99 | | | |
| stddev | | | |

### Latency by Level

#### sd

| Metric | 1x16_fm | 2x3_fm | 17x4_region_fm |
|--------|---------|--------|----------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

#### sgg

| Metric | 1x16_fm | 2x3_fm | 17x4_region_fm |
|--------|---------|--------|----------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

#### emd

| Metric | 1x16_fm | 2x3_fm | 17x4_region_fm |
|--------|---------|--------|----------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

---

## Local — Forcemerge X (nofm)

### Latency (Overall)

| Metric | lbt_1x16_nofm | lbt_2x3_nofm | lbt_17x4_region_nofm |
|--------|---------------|--------------|------------------------|
| avg | | | |
| p50 | | | |
| p90 | | | |
| p95 | | | |
| p99 | | | |
| stddev | | | |

### Latency by Level

#### sd

| Metric | 1x16_nofm | 2x3_nofm | 17x4_region_nofm |
|--------|-----------|----------|-------------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

#### sgg

| Metric | 1x16_nofm | 2x3_nofm | 17x4_region_nofm |
|--------|-----------|----------|-------------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

#### emd

| Metric | 1x16_nofm | 2x3_nofm | 17x4_region_nofm |
|--------|-----------|----------|-------------------|
| avg | | | |
| p50 | | | |
| p99 | | | |

---

## Remote

(원격 클러스터 세팅 후 기재)

---

## Findings

(결과 기록 후 분석)
