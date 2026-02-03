# Benchmark Overview

인덱스 구조: [INDEX.md](../INDEX.md) 참조

---

## Environment

| Env       | Description                              | OpenSearch | CPU              | Memory | Heap | Nodes |
|-----------|------------------------------------------|------------|------------------|--------|------|-------|
| **Local** | Docker single-node (macOS Apple Silicon) | 2.11.1     | 6 cores          | 16GB   | 8GB  | 1     |
| **INT**   | AWS OpenSearch Service                   | 2.11.1     | 2 vCPU (r6g.large) | 16GB   | 8GB  | 3     |

---

## Test Matrix

| # | Tag | Shards | Strategy | FM | Indices | Local | INT |
|---|-----|--------|----------|----|---------|-------|-----|
| 1 | `lbt_1x16_fm` | 16 | uniform | O | 1 | O | - |
| 2 | `lbt_1x16_nofm` | 16 | uniform | X | 1 | O | - |
| 3 | `lbt_2x3_fm` | 6 | uniform (hash) | O | 2 | O | O |
| 4 | `lbt_2x3_nofm` | 6 | uniform (hash) | X | 2 | O | - |
| 5 | `lbt_4x4_fm` | 16 | uniform (hash) | O | 4 | - | O |
| 6 | `lbt_17x4_region_fm` | 68 | region (SD) | O | 17 | O | O |
| 7 | `lbt_17x4_region_nofm` | 68 | region (SD) | X | 17 | O | - |

---

## Local

| File | Description |
|------|-------------|
| [INDEXING.md](local/INDEXING.md) | 인덱싱 결과 (시간, 문서수) |
| [VS_FORCEMERGE.md](local/VS_FORCEMERGE.md) | fm vs nofm 단일 쿼리 비교 |
| [VS_SHARD_STRATEGY.md](local/VS_SHARD_STRATEGY.md) | 샤드 전략 단일 쿼리 비교 |
| [LOAD_TEST.md](local/LOAD_TEST.md) | 동시 사용자 부하 테스트 |
| [RAW.md](local/RAW.md) | 원본 로그 데이터 |

## INT

| File | Description |
|------|-------------|
| [INDEXING.md](int/INDEXING.md) | 인덱싱 결과 (시간, 문서수) |
| [FORCEMERGE.md](int/FORCEMERGE.md) | forcemerge 소요시간 |
| [LOAD_TEST.md](int/LOAD_TEST.md) | 동시 사용자 부하 테스트 (nofm/fm) |

## Cross-Environment

| File | Description |
|------|-------------|
| [VS_ENVIRONMENT.md](VS_ENVIRONMENT.md) | 로컬 vs INT 환경 비교 |
