# Benchmark Overview

인덱스 구조: [INDEX.md](../INDEX.md) 참조

---

## Environment

| Env | Description | OpenSearch | CPU | Memory | Heap | Storage |
|-----|-------------|------------|-----|--------|------|---------|
| **Local** | Docker single-node (macOS Apple Silicon) | 2.11.1 | 6 cores | 16GB | 6GB | SSD |
| **Remote** | (TBD) | | | | | |

---

## Test Matrix

| # | Tag | Shards | Strategy | FM | Indices |
|---|-----|--------|----------|----|---------|
| 1 | `lbt_1x16_fm` | 16 | uniform | O | 1 |
| 2 | `lbt_1x16_nofm` | 16 | uniform | X | 1 |
| 3 | `lbt_2x3_fm` | 6 | uniform (hash) | O | 2 |
| 4 | `lbt_2x3_nofm` | 6 | uniform (hash) | X | 2 |
| 5 | `lbt_17x4_region_fm` | 68 | region (SD) | O | 17 |
| 6 | `lbt_17x4_region_nofm` | 68 | region (SD) | X | 17 |

---

## Files

| File | Comparison | Description |
|------|-----------|-------------|
| [INDEXING.md](INDEXING.md) | - | 인덱싱 결과 (시간, 문서수, 환경별) |
| [VS_FORCEMERGE.md](VS_FORCEMERGE.md) | fm vs nofm | 같은 구조, forcemerge 유무 비교 |
| [VS_SHARD_STRATEGY.md](VS_SHARD_STRATEGY.md) | 샤드 전략 | 1x16 vs 2x3 vs 17x4_region |
| [VS_ENVIRONMENT.md](VS_ENVIRONMENT.md) | 로컬 vs 원격 | 같은 인덱스, 환경 차이 비교 |
| [LOAD_TEST.md](LOAD_TEST.md) | 부하 테스트 | 동시 사용자별 RPS/지연 |
