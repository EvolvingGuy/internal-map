# Load Test (동시 사용자 부하 테스트)

---

## Test Setup

| Item | Value |
|------|-------|
| Tool | Locust (`locustfile.py`) |
| Stages | 5 (1 → 5 → 10 → 20 → 50 users) |
| Duration | 60s per stage |
| Ramp-up | Stage 1: instant, Stage 2-5: 5~10s |
| Cache | `request_cache=false` |
| Agg | sd / sgg / emd round-robin |

```bash
export ES_INDEX="{index_pattern}"

locust -f locustfile.py --host http://localhost:9200 \
  --headless -u {users} -r {ramp} -t 60s --csv results/{tag}_s{stage}
```

---

## Local

### lbt_1x16_fm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### lbt_1x16_nofm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### lbt_2x3_fm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### lbt_2x3_nofm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### lbt_17x4_region_fm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### lbt_17x4_region_nofm

| Users | RPS | avg | p50 | p90 | p95 | p99 | errors |
|-------|-----|-----|-----|-----|-----|-----|--------|
| 1 | | | | | | | |
| 5 | | | | | | | |
| 10 | | | | | | | |
| 20 | | | | | | | |
| 50 | | | | | | | |

### Saturation Point (Local)

| Tag | Saturate Users | Max RPS | p99 at saturation |
|-----|----------------|---------|-------------------|
| lbt_1x16_fm | | | |
| lbt_1x16_nofm | | | |
| lbt_2x3_fm | | | |
| lbt_2x3_nofm | | | |
| lbt_17x4_region_fm | | | |
| lbt_17x4_region_nofm | | | |

---

## Remote

(원격 클러스터 세팅 후 기재)
