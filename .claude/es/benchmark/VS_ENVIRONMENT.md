# 환경 비교 (Local vs Remote)

같은 인덱스를 로컬 Docker vs 원격 클러스터에서 비교.

---

## Environment Spec

| Item | Local | Remote |
|------|-------|--------|
| OpenSearch | 2.11.1 | (TBD) |
| Nodes | 1 (single-node) | (TBD) |
| CPU | 6 cores (Docker limit) | (TBD) |
| Memory | 16GB (Docker limit) | (TBD) |
| JVM Heap | 6GB | (TBD) |
| Storage | SSD (local) | (TBD) |
| Network | localhost | (TBD) |

---

## Indexing Time

| Tag | Local | Remote | Diff |
|-----|-------|--------|------|
| lbt_1x16_fm | | | |
| lbt_1x16_nofm | | | |
| lbt_2x3_fm | | | |
| lbt_2x3_nofm | | | |
| lbt_17x4_region_fm | | | |
| lbt_17x4_region_nofm | | | |

---

## Latency (Overall avg)

| Tag | Local | Remote | Diff |
|-----|-------|--------|------|
| lbt_1x16_fm | | | |
| lbt_1x16_nofm | | | |
| lbt_2x3_fm | | | |
| lbt_2x3_nofm | | | |
| lbt_17x4_region_fm | | | |
| lbt_17x4_region_nofm | | | |

---

## Latency (p99)

| Tag | Local | Remote | Diff |
|-----|-------|--------|------|
| lbt_1x16_fm | | | |
| lbt_1x16_nofm | | | |
| lbt_2x3_fm | | | |
| lbt_2x3_nofm | | | |
| lbt_17x4_region_fm | | | |
| lbt_17x4_region_nofm | | | |

---

## Load Test (50 users)

| Tag | Local RPS | Local p99 | Remote RPS | Remote p99 |
|-----|-----------|-----------|------------|------------|
| lbt_1x16_fm | | | | |
| lbt_1x16_nofm | | | | |
| lbt_2x3_fm | | | | |
| lbt_2x3_nofm | | | | |
| lbt_17x4_region_fm | | | | |
| lbt_17x4_region_nofm | | | | |

---

## Findings

(결과 기록 후 분석)
