# 벤치마크 02: Forcemerge 효과

---

## 한 줄 요약

**Forcemerge 하면 빨라진다. 세그먼트가 쪼개질수록 효과가 크다. 끝.**

---

## 핵심 발견: 세그먼트 쪼개짐 vs FM 효과

```
세그먼트 많이 쪼개짐  →  Forcemerge 효과 ↑↑↑
```

| Config | NoFM seg/shard | FM effect (RPS) | FM effect (latency) |
|--------|----------------|-----------------|---------------------|
| 4x4    | ~22            | **+43%**        | **-33%**            |
| 17x4   | ~7             | +30%            | -38%                |

**→ 쪼개진 정도에 비례해서 FM 효과가 커진다**

---

## 결과: 17x4 인덱스

### 순차 테스트

| Metric | FM    | NoFM  | Diff     |
|--------|-------|-------|----------|
| avg    | 58ms  | 94ms  | **-38%** |
| p50    | 44ms  | 70ms  | **-37%** |
| p95    | 168ms | 267ms | **-37%** |

### 부하 테스트

| Users | FM RPS | NoFM RPS | Diff       |
|-------|--------|----------|------------|
| 5     | 8.5    | 6.8      | **+26%**   |
| 10    | 10.9   | 8.8      | **+23%**   |
| 15    | 12.3   | 9.0      | **+37%**   | peak
| 20    | 11.4   | 8.0      | **+44%**   |
| 25    | 10.5   | 9.0      | **+17%**   |

**평균 ~30% 처리량 향상**

---

## 결과: 4x4 인덱스

### 순차 테스트

| Metric | FM    | NoFM  | Diff     |
|--------|-------|-------|----------|
| avg    | 95ms  | 142ms | **-33%** |
| p50    | 46ms  | 76ms  | **-39%** |
| p95    | 503ms | 662ms | **-24%** |

### 부하 테스트

| Users | FM RPS | NoFM RPS | Diff       |
|-------|--------|----------|------------|
| 5     | 12.5   | 8.9      | **+40%**   |
| 10    | 11.9   | 8.4      | **+42%**   |
| 15    | 12.4   | 8.6      | **+43%**   |
| 20    | 12.4   | 8.6      | **+44%**   |
| 25    | 12.4   | 8.5      | **+46%**   |

**평균 ~43% 처리량 향상 (17x4보다 효과 큼!)**

### 4x4 특이점: Saturation 없음

```
17x4: 15명 피크 후 하락  vs  4x4: 25명까지 안정 (~12 RPS)
```

4x4는 인덱스 수가 적어서 (4개 vs 17개) 부하 분산이 더 잘 됨

---

## 두 테스트가 왜 필요한가?

| Test     | Goal           | Measure                    |
|----------|----------------|----------------------------|
| Seq      | one at a time  | single query latency       |
| Load     | many at once   | system throughput (RPS)    |

### 쉬운 비유

- **순차 테스트**: 식당에 손님 1명. 음식 나오는 시간 측정
- **부하 테스트**: 식당에 손님 25명. 주방이 터지는지 측정

둘 다 해야 **"빠르고 + 많이 처리"** 할 수 있는지 알 수 있음.

---

## 최종 결론

| Case     | FM          | NoFM             | Winner |
|----------|-------------|------------------|--------|
| single   | fast        | slow             | FM     |
| multiple | fast + high | slow + low       | FM     |

**→ 어떤 상황에서도 FM이 이김. Forcemerge 안 할 이유 없음.**

---

## 권장사항

1. **인덱싱 끝나면 무조건 forcemerge 해라**
2. 프로덕션에서는 한가한 시간에 실행
3. 계속 쓰기가 있으면 주기적으로 forcemerge

---

## 테스트 환경

### 17x4 (17개 인덱스 × 4샤드)

| Item      | FM           | NoFM            |
|-----------|--------------|-----------------|
| Indices   | 17           | 17              |
| Shards    | 68 total     | 68 total        |
| Segments  | 68 (1/shard) | 467 (~7/shard)  |
| Docs      | ~88M         | ~88M            |

### 4x4 (4개 인덱스 × 4샤드)

| Item      | FM           | NoFM            |
|-----------|--------------|-----------------|
| Indices   | 4            | 4               |
| Shards    | 16 total     | 16 total        |
| Segments  | 16 (1/shard) | 346 (~22/shard) |
| Docs      | ~88M         | ~88M            |

---

## 테스트 방법 상세

### 순차 테스트 설계

**왜 이렇게 했나**: 외부 요인 통제해서 공정하게 비교

- 두 ES 동시에 띄움 (포트 9200, 9201)
- 쿼리마다 FM → NoFM 번갈아 실행
- 순서도 50:50으로 섞음 (FM 먼저 / NoFM 먼저)
- 500개 쿼리 × 9회 반복 = 4500회씩

### 부하 테스트 설계

**왜 이렇게 했나**: 실제 서비스처럼 동시 요청 상황 시뮬레이션

- 동시 유저 5 → 10 → 15 → 20 → 25명 단계적 증가
- 각 단계에서 30초간 측정
- 처리량(RPS)과 응답시간 동시 측정
- Saturation point 찾기 위해 세밀한 간격 (5명씩)

---

## Forcemerge 주의사항

### 언제 하면 안 되나?

| Case              | Reason                         | Alternative        |
|-------------------|--------------------------------|--------------------|
| During writes     | new segments keep appearing    | run after indexing |
| Right after index | docs may still be in memory    | flush first        |
| Peak hours        | high CPU/IO impact             | run off-peak       |

### 효과가 줄어드는 경우

1. **쓰기가 잦은 인덱스**: 새 문서 → 새 세그먼트 → forcemerge 무의미
2. **이미 세그먼트 적은 경우**: 애초에 병합할 게 없음
3. **SSD 환경**: HDD보다 효과 체감 적음 (그래도 효과는 있음)

### 권장 사용 패턴

```
[인덱싱] → [flush] → [forcemerge] → [읽기 전용으로 사용]
```

---

## Saturation Point 분석

### 17x4 vs 4x4 처리량 곡선

```
RPS   17x4 FM                    4x4 FM
 13 |                        *---*---*---*---*
 12 |           *
 11 |       *       *
 10 |   *               *
  9 |
  8 |
    +---+---+---+---+---+    +---+---+---+---+---+
        5  10  15  20  25        5  10  15  20  25

    [피크 후 하락]              [안정 유지]
```

### 17x4: Saturation 발생

| Range  | Result        | Reason                             |
|--------|---------------|------------------------------------|
| 5-15   | RPS up        | more concurrency, better util      |
| **15** | **peak 12.3** | **optimal concurrency**            |
| 15-25  | RPS down      | contention, queuing delay          |

### 4x4: Saturation 없음

| Range  | Result        | Reason                             |
|--------|---------------|------------------------------------|
| 5-25   | stable ~12    | fewer indices, less coordination   |

### 왜 다른가?

- **17x4**: 17개 인덱스 동시 검색 → 코디네이션 오버헤드 큼
- **4x4**: 4개 인덱스만 검색 → 오버헤드 적음 → 안정적

---

## 관련 파일

### 스크립트
- 순차 테스트: `benchmark/fm_benchmark_paired.py`
- 부하 테스트: `benchmark/saturation_test.py`
- 쿼리셋: `benchmark/queryset_sd_500.json`
- Docker: `docker-compose.benchmark.yml`

### 결과 (17x4)
- 순차: `benchmark/results/paired_sd_500_9rounds.json`
- 부하: `benchmark/results/saturation_test_v2.json`

### 결과 (4x4)
- 순차: `benchmark/results/paired_4x4_9rounds.json`
- 부하: `benchmark/results/saturation_4x4.json`
