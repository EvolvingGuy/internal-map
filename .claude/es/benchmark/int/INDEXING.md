# Indexing (INT)

## Environment

| Item          | Value                                   |
|---------------|-----------------------------------------|
| OpenSearch    | 2.11.1 (AWS OpenSearch Service)         |
| Instance      | r6g.large (2 vCPU, 16GB)                |
| Nodes         | 3                                       |
| JVM Heap      | 8GB (per node)                          |
| Storage       | EBS                                     |

## Dataset

| Item     | Count      |
|----------|------------|
| Land     | 39,668,369 | 토지 (필지)
| Building | 6,210,982  | 건축물    
| Trade    | 41,844,124 | 실거래    

## Indexing Results

| Structure        | Indices | Shards | Total Shards | Indexing Time  | Bulks  |
|------------------|---------|--------|--------------|----------------|--------|
| 17x4_region_fm   | 17      | 4      | 68           | 26.09m (1565s) | 42,338 |
| 4x4_fm           | 4       | 4      | 16           | 52.51m (3150s) | 42,338 |
| 2x3_fm           | 2       | 3      | 6            | 38.96m (2337s) | 42,338 |

> 17x4가 4x4 대비 **2배**, 2x3 대비 **1.5배** 빠름.

### Note: 인덱싱 시간 비교 시 주의사항

인덱싱 속도 차이에는 **구조적 요인**과 **구현 방식 요인**이 혼재되어 있다.

**17x4_region (region 파티셔닝)**
- DB fetch 단위(EMD)와 인덱스 단위(시도)가 자연스럽게 일치
- 1 EMD 배치 → 1 인덱스로 bulk 1회. 분할 오버헤드 없음
- 68 primary shards가 3노드에 분산되어 write 병렬성 극대화

**4x4 / 2x3 (hash 파티셔닝)**
- DB fetch 단위(EMD)와 인덱스 단위(hash partition)가 불일치
- 1 EMD 배치 → PNU hash로 분류 → 파티션 수만큼 bulk 분할 (4x4는 4회, 2x3은 2회)
- bulk당 문서 수 감소 + API 호출 증가 → 네트워크 I/O 오버헤드
- 파티션별 독립 fetch가 불가능 (DB에 hash 파티션 컬럼 없음)

따라서 4x4와 2x3의 인덱싱 속도는 구현 최적화(비동기 병렬 bulk, 버퍼 누적 flush 등)로 개선 가능성이 있으며, 현재 수치가 구조 자체의 한계를 의미하지는 않는다. 17x4의 인덱싱 우위는 region 파티셔닝의 구조적 이점이 확실하다.

### lbt_17x4_region_fm (26.09m)

| Region (SD) | Index Name              | Docs      |
|-------------|-------------------------|-----------|
| 11          | lbt_17x4_region_fm_11   | 903,166   | 서울
| 26          | lbt_17x4_region_fm_26   | 714,165   | 부산
| 27          | lbt_17x4_region_fm_27   | 791,389   | 대구
| 28          | lbt_17x4_region_fm_28   | 669,158   | 인천
| 29          | lbt_17x4_region_fm_29   | 390,256   | 광주
| 30          | lbt_17x4_region_fm_30   | 292,049   | 대전
| 31          | lbt_17x4_region_fm_31   | 507,628   | 울산
| 36          | lbt_17x4_region_fm_36   | 206,484   | 세종
| 41          | lbt_17x4_region_fm_41   | 5,164,227 | 경기
| 43          | lbt_17x4_region_fm_43   | 2,395,086 | 충북
| 44          | lbt_17x4_region_fm_44   | 3,740,187 | 충남
| 46          | lbt_17x4_region_fm_46   | 5,899,035 | 전남
| 47          | lbt_17x4_region_fm_47   | 5,694,523 | 경북
| 48          | lbt_17x4_region_fm_48   | 4,819,948 | 경남
| 50          | lbt_17x4_region_fm_50   | 880,280   | 제주
| 51          | lbt_17x4_region_fm_51   | 2,729,990 | 강원
| 52          | lbt_17x4_region_fm_52   | 3,870,798 | 전북

### lbt_4x4_fm (52.51m)

| Partition | Index Name   | Docs      |
|-----------|--------------|-----------|
| P1        | lbt_4x4_fm_1 | 9,919,172 |
| P2        | lbt_4x4_fm_2 | 9,916,515 |
| P3        | lbt_4x4_fm_3 | 9,916,007 |
| P4        | lbt_4x4_fm_4 | 9,916,675 |

### lbt_2x3_fm (38.96m)

| Partition | Index Name   | Docs       |
|-----------|--------------|------------|
| P1        | lbt_2x3_fm_1 | 19,835,179 |
| P2        | lbt_2x3_fm_2 | 19,833,190 |
