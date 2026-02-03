# Forcemerge 효과 비교 (fm vs nofm)

같은 샤드 구조에서 forcemerge 유무만 다른 쌍 비교.

---

## Test Setup

| Item       | Value                                                    |
|------------|----------------------------------------------------------|
| Tool       | `VS_FORCEMERGE_BENCH.py`                                 |
| Rounds     | 2,000                                                    |
| Agg Levels | sd / sgg / emd (round-robin, ~667 each)                  |
| Cache      | `request_cache=false`                                    |
| Bbox       | Nationwide + jitter (cache bypass)                       |
| Filter     | Random combination (land 60%, building 50%, trade 50%)   |

```bash
python3 VS_FORCEMERGE_BENCH.py \
  --url http://localhost:9200 \
  --index "{index_pattern}" \
  --tag {tag} \
  --rounds 2000
```

---

## Local

### 1x16: fm vs nofm

| Metric | lbt_1x16_fm | lbt_1x16_nofm   | Diff   |
|--------|-------------|-----------------|--------|
| avg    | 130.3ms     | 151.1ms         | -13.8% |
| p50    | 60ms        | 88ms            | -31.8% |
| p90    | 318ms       | 378ms           | -15.9% |
| p95    | 657ms       | 556ms           | +18.2% |
| p99    | 969ms       | 805ms           | +20.4% |
| stddev | 199.8ms     | 172.4ms         | +15.9% |
| min    | 3ms         | 4ms             | -25.0% |
| max    | 1364ms      | 1079ms          | +26.4% |

### 2x3: fm vs nofm

| Metric | lbt_2x3_fm | lbt_2x3_nofm   | Diff   |
|--------|------------|----------------|--------|
| avg    | 108.3ms    | 128.4ms        | -15.7% |
| p50    | 58ms       | 72ms           | -19.4% |
| p90    | 258ms      | 273ms          | -5.5%  |
| p95    | 494ms      | 574ms          | -13.9% |
| p99    | 667ms      | 790ms          | -15.6% |
| stddev | 145.0ms    | 165.8ms        | -12.5% |
| min    | 3ms        | 4ms            | -25.0% |
| max    | 1034ms     | 1192ms         | -13.3% |

### 17x4_region: fm vs nofm

| Metric | lbt_17x4_region_fm | lbt_17x4_region_nofm   | Diff   |
|--------|--------------------|------------------------|--------|
| avg    | 68.9ms             | 107.5ms                | -35.9% |
| p50    | 40ms               | 72ms                   | -44.4% |
| p90    | 148ms              | 255ms                  | -42.0% |
| p95    | 315ms              | 376ms                  | -16.2% |
| p99    | 395ms              | 497ms                  | -20.5% |
| stddev | 88.1ms             | 104.2ms                | -15.5% |
| min    | 2ms                | 4ms                    | -50.0% |
| max    | 597ms              | 725ms                  | -17.7% |

---

## Remote

(원격 클러스터 세팅 후 기재)

---

## Findings

- **forcemerge는 전반적으로 avg/p50 기준 13~36% 성능 향상** 효과
- **17x4_region에서 가장 큰 효과**: avg -35.9%, p50 -44.4% (region 분리 + forcemerge 시너지)
- **2x3에서 안정적 개선**: 전 구간에서 5~16% 감소
- **1x16은 p50까지는 개선되나 tail latency(p95~max)에서 fm이 오히려 높음**: 단일 인덱스 16샤드 구조에서 forcemerge 후 일부 대형 세그먼트가 tail에 영향을 줄 수 있음
- **region 기반 분리(17x4)가 forcemerge 효과를 극대화**: 각 region 인덱스가 작아 merge 후 세그먼트가 최적화되기 쉬움
