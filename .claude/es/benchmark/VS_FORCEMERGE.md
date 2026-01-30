# Forcemerge 효과 비교 (fm vs nofm)

같은 샤드 구조에서 forcemerge 유무만 다른 쌍 비교.

---

## Test Setup

| Item | Value |
|------|-------|
| Tool | `VS_FORCEMERGE_BENCH.py` |
| Rounds | 2,000 |
| Agg Levels | sd / sgg / emd (round-robin, ~667 each) |
| Cache | `request_cache=false` |
| Bbox | Nationwide + jitter (cache bypass) |
| Filter | Random combination (land 60%, building 50%, trade 50%) |

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

| Metric | lbt_1x16_fm | lbt_1x16_nofm | Diff |
|--------|-------------|----------------|------|
| avg | | | |
| p50 | | | |
| p90 | | | |
| p95 | | | |
| p99 | | | |
| stddev | | | |
| min | | | |
| max | | | |

### 2x3: fm vs nofm

| Metric | lbt_2x3_fm | lbt_2x3_nofm | Diff |
|--------|------------|---------------|------|
| avg | | | |
| p50 | | | |
| p90 | | | |
| p95 | | | |
| p99 | | | |
| stddev | | | |
| min | | | |
| max | | | |

### 17x4_region: fm vs nofm

| Metric | lbt_17x4_region_fm | lbt_17x4_region_nofm | Diff |
|--------|--------------------|-----------------------|------|
| avg | | | |
| p50 | | | |
| p90 | | | |
| p95 | | | |
| p99 | | | |
| stddev | | | |
| min | | | |
| max | | | |

---

## Remote

(원격 클러스터 세팅 후 기재)

---

## Findings

(결과 기록 후 분석)
