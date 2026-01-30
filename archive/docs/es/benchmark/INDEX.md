# Indexing & Forcemerge

인덱싱 앱은 항상 **로컬 JVM**(Spring Boot)에서 실행. 대상 OpenSearch 클러스터만 다름.

## Environment Spec

### local

로컬 Docker 단일 노드.

| Item | Spec |
|------|------|
| OpenSearch | Docker (단일 노드) |
| CPU  | 14 cores |
| Heap | 6GB |
| Disk | SSD (macOS) |
| Network | localhost (지연 없음) |

### cluster (AWS)

SSH 터널링을 통한 원격 AWS OpenSearch.

| Item | Spec |
|------|------|
| OpenSearch | AWS OpenSearch Service |
| Access | SSH tunnel → `https://localhost:50505` |
| Network | 로컬 JVM → SSH tunnel → AWS (네트워크 지연 있음) |

> 인덱싱 앱은 로컬 JVM에서 실행되므로, cluster 대상 인덱싱 시 네트워크 지연이 소요시간에 포함됨.

## Index/Shard 구조

| Tag     | Index | Shards/Index | Total Shards | Docs/Shard     |
|---------|-------|--------------|--------------|----------------|
| 1/16    | 1     | 16           | 16           | ~5,480,000     | lnbtp16 (단일 인덱스 16샤드)
| 4/4     | 4     | 4            | 16           | ~5,480,000     | lnbtpu (4 파티션 균등분배)
| 2/3     | 2     | 3            | 6            | ~14,600,000    | lnbtpu23 (2 파티션 균등분배)
| 17/2    | 17    | 2            | 34           | ~2,580,000     | lnbtps (시도별 분할)

총 데이터: 87,723,475 Lucene docs (39,668,369 필지 + nested buildings/trades)

모든 인덱스는 nested(buildings, trades) 구조 동일, replica 0.

## 인덱싱 소요시간

### local

| Tag  | Total Time       |
|------|------------------|
| 1/16 | 38.52m (2311.4s) | [LNBTP16] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 38.52m (2311.4s)
| 4/4  | 34.35m (2060.8s) | [LNBTPU23] 총 문서: 39,668,369건, 총 건물: 6,210,982건, 총 실거래: 41,844,124건, 벌크 42,338회, 총 소요시간: 34.35m (2060.8s) 파티션별 문서 수: P1=19,835,179, P2=19,833,190
| 2/3  |                  |
| 17/2 | 32.06m (1923.6s) |

### cluster

| Tag  | Total Time |
|------|------------|
| 1/16 |            |
| 4/4  |            |
| 2/3  |            |
| 17/2 |            |

## Forcemerge 소요시간

wall-clock 기준 (요청 ~ 전체 완료). 샤드별 병렬 처리되므로 가장 느린 샤드 기준.

### local

| Tag  | Total Time        |
|------|-------------------|
| 1/16 | 4.10m (245,964ms) |
| 4/4  |                   |
| 2/3  | 3.43m (205,684ms) | [LNBTPU23] forcemerge 전체 완료: 3.43m (205,684ms) - 비병렬
| 17/2 |                   |

### cluster

| Tag  | Total Time |
|------|------------|
| 1/16 |            |
| 4/4  |            |
| 2/3  |            |
| 17/2 |            |
