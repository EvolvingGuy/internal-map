# Load Test (동시 사용자 부하 테스트)

## Performance Ranking

| Rank | Structure            | Max RPS | Saturate | avg @u40 | p99 @u40 | avg @u50 | p99 @u50 |
|------|----------------------|---------|----------|----------|----------|----------|----------|
| 1    | 17x4_region_fm       | 13.0    | ~50      | 132ms    | 730ms    | 364ms    | 2700ms   |
| 2    | 2x3_fm               | 12.0    | ~40      | 312ms    | 2100ms   | 670ms    | 2900ms   |
| 3    | 1x16_fm              | 10.8    | ~40      | 442ms    | 2500ms   | 1074ms   | 4100ms   |
| 4    | 2x3_nofm             | 10.2    | ~30      | 737ms    | 3200ms   | 1418ms   | 5200ms   |
| 5    | 1x16_nofm            | 8.8     | ~20      | 1869ms   | 6200ms   | 1978ms   | 7900ms   |
| 6    | 17x4_region_nofm     | 8.5     | ~20      | 1313ms   | 4800ms   | 2366ms   | 7800ms   |

> 전국 bbox 기준 worst-case 측정. 실 서비스(줌 레벨별 좁은 bbox)에서는 이보다 빠름.
> 4x4(총 16샤드)는 미측정이나, 동일 샤드 수인 1x16과 유사하거나 파티셔닝 이점으로 소폭 우위로 추정.

### Conclusion

- **forcemerge는 필수**: fm/nofm 격차가 총 샤드 수에 비례하여 증폭됨 (17x4: ~10x, 1x16: ~4.2x, 2x3: ~2.4x). 동시 사용자가 많은 실서버에서는 이 격차가 더 커짐
- **총 샤드 수(인덱스 × 샤드)가 성능을 결정**: fm 적용 시 샤드가 많을수록 병렬성이 높아져 유리하고, nofm에서는 샤드가 많을수록 segment 파일 경합이 곱으로 늘어 불리
- **nofm이면 샤드를 줄여야 함**: nofm 기준 2x3(6샤드)이 saturate ~30으로 가장 오래 버틴 반면, 17x4(68샤드)는 ~20에서 이미 포화
- **fm + 다샤드 조합이 최적**: 17x4_region_fm은 u=40까지 avg 132ms로 거의 열화 없이 유일하게 RPS 13.0 돌파

---

## Test Design

### 시나리오: 지도 기반 부동산 웹 서비스

사용자가 웹 지도에서 필지/건물/실거래 정보를 조회하는 시나리오를 시뮬레이션한다.

**사용자 행동 패턴:**
1. 지도를 이동/줌 → viewport에 해당하는 `geo_bounding_box` 쿼리 발생
2. 결과(마커, 클러스터)를 확인 → **2~5초 대기** (think time)
3. 필터 변경 또는 지도 재이동 → 다음 쿼리 발생
4. 반복

이 패턴은 지도 기반 웹 서비스에서 가장 일반적인 사용 흐름이며, think time 2~5초는 사용자가 지도 결과를 시각적으로 인식하고 다음 동작을 결정하는 데 걸리는 평균 시간이다.

### 쿼리 구성

| key             |                                                         
|-----------------|
| geo             | `geo_bounding_box` (전국 범위 + jitter)                           
| land filter     | 용도지역, 지목, 면적, 공시지가 (60% 확률)                                   
| building filter | 용도, 등기, 연면적, 사용승인일 (50% 확률, nested)                           
| trade filter    | 매물유형, 계약일, 거래금액 (50% 확률, nested)                              
| Aggregation     | `terms` agg (sd/sgg/emd round-robin) + `geo_centroid` sub-agg 
| result response | `size: 0` (집계만, hits 미반환)                                     
| cache           | `request_cache=false` (cold 성능 측정)                            

매 쿼리마다 필터 조합이 랜덤 생성되어 캐시 우회 + 다양한 워크로드를 재현한다.

### 왜 이 설정이 지도 서비스에 적합한가

| elements                  | choice                   |
|---------------------------|--------------------------|
| Think time                | 2~5 second               | 지도 pan/zoom 후 결과를 시각적으로 확인하는 인지 시간             
| Bbox range                | Nationwide + jitter      | 최악의 경우(전국 뷰)를 기준으로 측정, 실 서비스에서 더 좁은 범위는 이보다 빠름 
| Filter random combination | Probability-based on/off | 실 사용자마다 다른 필터 조건을 적용하는 상황 재현                   
| Cache disabled            | each query               | `request_cache=false`로 shard-level 캐시 비활성화 + jitter/랜덤 필터로 filesystem cache, Lucene 내부 캐시까지 이중 차단
| Agg round-robin           | sd/sgg/emd               | 줌 레벨에 따라 시도→시군구→읍면동 전환하는 패턴 재현                 

---

## Test Setup

### 환경

| Item              | Value                           |
|-------------------|---------------------------------|
| OpenSearch        | 2.11.1 (Docker single-node)     |
| CPU               | 6 cores                         |
| Memory            | 16GB (15.6GB)                   |
| JVM Heap          | 8GB                             |
| OS                | macOS (Docker Desktop)          |

### 부하 설정

| Item          |
|---------------|
| Tool          | Locust 2.34.0 (`locustfile.py`) 
| Think Time    | 2~5 second (between)            
| Spawn Rate    | 10 users/sec                    
| Duration      | 60초 per stage                   
| User Levels   | 5 / 10 / 20 / 30 / 40 / 50      
| Cache         | `request_cache=false`           
| Agg Levels    | sd / sgg / emd (round-robin)    
| Warmup        | 5초 (1 user, 측정 미포함)

### 비계단식 독립 테스트

각 유저 수(5/10/20/30/40/50)는 **독립된 별도 테스트**로 실행된다. 계단식(step load)처럼 유저를 점진적으로 누적하는 방식이 아니라, 매 테스트마다 컨테이너를 재시작하고 해당 유저 수만으로 60초간 측정한다.

```
[컨테이너 시작] → [헬스체크] → [워밍업 5초] → [u=5 독립 60초] → [컨테이너 중지]
[컨테이너 시작] → [헬스체크] → [워밍업 5초] → [u=10 독립 60초] → [컨테이너 중지]
...
[컨테이너 시작] → [헬스체크] → [워밍업 5초] → [u=50 독립 60초] → [컨테이너 중지]
```

이 방식의 장점:
- 이전 단계의 부하가 다음 단계에 영향을 주지 않음 (JVM GC 압박, 캐시 오염 등 배제)
- 각 유저 수에서의 **순수 성능**을 측정할 수 있음
- 컨테이너 재시작으로 매번 동일한 초기 상태에서 출발

### 예상 QPS (think time 2~5초 기준)

| Users | Estimated QPS range    |
|-------|------------------------|
| 5     | ~1.4 – 2.5             |
| 10    | ~2.9 – 5.0             |
| 20    | ~5.7 – 10.0            |
| 30    | ~8.6 – 15.0            |
| 40    | ~11.4 – 20.0           |
| 50    | ~14.3 – 25.0           |

```bash
ES_INDEX="{index_pattern}" python3 -m locust \
  -f archive/docs/es/compare/locustfile.py \
  --host http://localhost:9200 \
  --headless -u {users} -r 10 -t 60s \
  --csv results/{tag}_u{users}
```

---

## Local

### lbt_1x16_fm

| Users | RPS  | avg     | p50   | p90    | p95    | p99    | errors |
|-------|------|---------|-------|--------|--------|--------|--------|
| 5     | 1.3  | 284ms   | 140ms | 590ms  | 1700ms | 1900ms | 0      |
| 10    | 2.9  | 206ms   | 100ms | 550ms  | 990ms  | 1200ms | 0      |
| 20    | 5.6  | 134ms   | 76ms  | 350ms  | 500ms  | 730ms  | 0      |
| 30    | 8.2  | 187ms   | 100ms | 490ms  | 690ms  | 1000ms | 0      |
| 40    | 10.1 | 442ms   | 200ms | 1300ms | 1600ms | 2500ms | 0      |
| 50    | 10.8 | 1074ms  | 740ms | 3000ms | 3700ms | 4100ms | 0      |

### lbt_1x16_nofm

| Users | RPS  | avg     | p50    | p90    | p95    | p99    | errors |
|-------|------|---------|--------|--------|--------|--------|--------|
| 5     | 1.4  | 264ms   | 140ms  | 820ms  | 1000ms | 1100ms | 0      |
| 10    | 2.8  | 166ms   | 90ms   | 590ms  | 710ms  | 770ms  | 0      |
| 20    | 5.2  | 403ms   | 250ms  | 1000ms | 1100ms | 1600ms | 0      |
| 30    | 6.9  | 780ms   | 530ms  | 1800ms | 2300ms | 3200ms | 0      |
| 40    | 7.5  | 1869ms  | 1300ms | 4100ms | 6000ms | 6200ms | 0      |
| 50    | 8.8  | 1978ms  | 1200ms | 5500ms | 7200ms | 7900ms | 0      |

### lbt_2x3_fm

| Users | RPS  | avg    | p50   | p90     | p95    | p99    | errors |
|-------|------|--------|-------|---------|--------|--------|--------|
| 5     | 1.4  | 218ms  | 97ms  | 530ms   | 770ms  | 1100ms | 0      |
| 10    | 2.8  | 145ms  | 93ms  | 360ms   | 410ms  | 740ms  | 0      |
| 20    | 5.7  | 150ms  | 75ms  | 490ms   | 640ms  | 930ms  | 0      |
| 30    | 8.1  | 190ms  | 110ms | 500ms   | 640ms  | 950ms  | 0      |
| 40    | 10.4 | 312ms  | 140ms | 900ms   | 1400ms | 2100ms | 0      |
| 50    | 12.0 | 670ms  | 290ms | 1900ms  | 2400ms | 2900ms | 0      |

### lbt_2x3_nofm

| Users | RPS  | avg     | p50   | p90    | p95    | p99    | errors |
|-------|------|---------|-------|--------|--------|--------|--------|
| 5     | 1.4  | 273ms   | 140ms | 850ms  | 1100ms | 1400ms | 0      |
| 10    | 2.7  | 256ms   | 120ms | 900ms  | 1000ms | 1400ms | 0      |
| 20    | 5.4  | 322ms   | 140ms | 770ms  | 1100ms | 2100ms | 0      |
| 30    | 7.8  | 438ms   | 210ms | 1200ms | 1600ms | 2200ms | 0      |
| 40    | 9.6  | 737ms   | 470ms | 1800ms | 2100ms | 3200ms | 0      |
| 50    | 10.2 | 1418ms  | 960ms | 3900ms | 4400ms | 5200ms | 0      |

### lbt_17x4_region_fm

| Users | RPS  | avg    | p50   | p90   | p95    | p99    | errors |
|-------|------|--------|-------|-------|--------|--------|--------|
| 5     | 1.4  | 143ms  | 67ms  | 380ms | 750ms  | 810ms  | 0      |
| 10    | 2.8  | 141ms  | 72ms  | 440ms | 550ms  | 620ms  | 0      |
| 20    | 5.7  | 111ms  | 59ms  | 320ms | 460ms  | 620ms  | 0      |
| 30    | 8.5  | 120ms  | 59ms  | 340ms | 450ms  | 660ms  | 0      |
| 40    | 11.2 | 132ms  | 67ms  | 350ms | 500ms  | 730ms  | 0      |
| 50    | 13.0 | 364ms  | 220ms | 790ms | 1300ms | 2700ms | 0      |

### lbt_17x4_region_nofm

| Users | RPS  | avg     | p50    | p90    | p95    | p99    | errors |
|-------|------|---------|--------|--------|--------|--------|--------|
| 5     | 1.4  | 186ms   | 88ms   | 390ms  | 1000ms | 1600ms | 0      |
| 10    | 2.9  | 188ms   | 120ms  | 580ms  | 620ms  | 840ms  | 0      |
| 20    | 5.3  | 438ms   | 210ms  | 1200ms | 2000ms | 2400ms | 0      |
| 30    | 7.3  | 724ms   | 490ms  | 1800ms | 2000ms | 2800ms | 0      |
| 40    | 8.2  | 1313ms  | 780ms  | 3400ms | 3800ms | 4800ms | 0      |
| 50    | 8.5  | 2366ms  | 2000ms | 4800ms | 6700ms | 7800ms | 0      |

### Saturation Point (Local)

RPS가 정체되고 p99가 급격히 증가하는 시점을 포화점으로 판단.

| Tag                   | Saturate Users | Max RPS | p99 at saturation |
|-----------------------|----------------|---------|-------------------|
| lbt_1x16_fm           | ~40            | 10.8    | 2500ms            |
| lbt_1x16_nofm         | ~20            | 8.8     | 1600ms            |
| lbt_2x3_fm            | ~40            | 12.0    | 2100ms            |
| lbt_2x3_nofm          | ~30            | 10.2    | 2200ms            |
| lbt_17x4_region_fm    | ~50            | 13.0    | 2700ms            |
| lbt_17x4_region_nofm  | ~20            | 8.5     | 2400ms            |

### Findings

- **17x4_region_fm이 부하 테스트에서도 압도적**: u=40까지 avg 132ms, p50 67ms로 거의 저하 없음. 유일하게 u=50에서도 RPS 13.0 달성
- **forcemerge 효과가 부하 시 극대화**: fm은 높은 유저 수에서도 안정적이나, nofm은 u=20부터 급격히 열화
- **17x4_region_nofm은 단일 쿼리에서는 빨랐으나 부하 시 급격히 열화**: u=20에서 이미 avg 438ms, u=50에서 avg 2366ms
- **RPS 상한**: 로컬 싱글 노드 기준 최대 ~13 RPS (17x4_fm), 1x16_nofm은 ~8.8 RPS에서 정체
- **에러 0건**: 모든 구조에서 timeout이나 에러 없이 응답. 성능 열화는 있으나 장애는 없음


