# Forcemerge 성능 비교 벤치마크

## Forcemerge란
세그먼트(segment)는 Lucene이 데이터를 저장하는 불변 파일 단위다.
bulk 인덱싱 시 매 배치마다 새로운 세그먼트가 생성되어, 인덱싱이 끝나면 샤드당 수십 개의 세그먼트가 남는다.
검색 시 모든 세그먼트를 각각 순회한 뒤 결과를 병합해야 하므로, 세그먼트가 많을수록 오버헤드가 증가한다.
forcemerge는 이 분산된 세그먼트들을 1개로 합쳐서 검색 오버헤드를 줄이는 작업이다.
**가설: 세그먼트를 1개로 합치면 집계 쿼리 성능이 유의미하게 개선될 것이다.**

## 결론
**forcemerge는 모든 지표에서 일관된 성능 향상을 제공한다.**

| Metric   | Forcemerged (1 seg) | No Forcemerge (16~22 segs) |
|----------|---------------------|----------------------------|
| avg      | 16ms                | 35ms                       | 2.2x
| p50      | 5ms                 | 21ms                       | 4.2x
| p90      | 35ms                | 69ms                       | 2.0x
| p95      | 58ms                | 111ms                      | 1.9x
| p99      | 176ms               | 282ms                      | 1.6x
| stddev   | 40.5ms              | 56.0ms                     | -38%
| total    | 37.5s               | 78.5s                      | 2.1x

- 평균 2.2배, 중앙값 4.2배 빠름
- 전 구간에서 forcemerged가 우위 (예외 없음)
- 응답 시간 변동성도 낮아져 더 예측 가능한 성능
- **인덱싱 완료 후 `_forcemerge?max_num_segments=1` 필수**

일부에서는 forcemerge가 오히려 성능에 불리하다는 의견이 있으나,
이는 데이터가 적어 차이가 체감되지 않는 환경이거나 표본이 부족한 테스트에서 비롯된 것으로 보인다.
구조적으로 세그먼트 수를 줄이면 순회 대상이 줄어드는 것이므로, 읽기 전용 인덱스에서 손해가 날 수 없다.
우리 환경에서의 테스트에서는 2,000회 전 구간에서 예외 없이 forcemerge 처리가 되어 segment가 1인 게 우위

---

## 어떤 테스트를 했는가
동일한 데이터(87,723,475 문서)를 가진 두 인덱스에 대해
**다양한 필터 조합을 랜덤 생성하여 각각 2,000회 쿼리**를 실행했다.
- **forcemerged**: 샤드당 세그먼트 1개 (lnbtpu_*)
- **no forcemerge**: 샤드당 세그먼트 16~22개 (lnbtpunf_*)
- 두 인덱스는 세그먼트 병합 여부만 다르고, 데이터/구조/샤드 수 모두 동일

### 표본

| Item           | Value                              |
|----------------|------------------------------------|
| samples        | 2,000 each (4,000 total)           | 각 인덱스 2,000회
| filter combos  | 2,000 unique random combinations   | 모두 서로 다른 필터 조합
| cache          | `request_cache=false`              | 매번 풀스캔 강제
| errors         | 0 / 0                              |

### 필터 조합 방식

매 쿼리마다 아래 요소를 랜덤 조합하여 **매번 다른 쿼리**를 생성:

- **지역 (BBox)**: 전국/서울/경기/부산/대전/광주/대구/인천/세종/제주 중 1개
- **집계 레벨**: SD(시도) / SGG(시군구) / EMD(읍면동) 중 1개
- **토지 필터**: 용도지역, 지목, 면적 범위, 공시지가 범위
- **건물 필터 (nested)**: 건물용도, 대장구분, 연면적 범위, 사용승인일 범위, 최근5년 허가/착공
- **실거래 필터 (nested)**: 거래유형, 계약일 범위, 거래금액 범위, 건물 단가 범위

각 카테고리(토지/건물/실거래)는 60%/50%/50% 확률로 활성화되며,
활성화된 카테고리 내 개별 필터도 각각 독립 확률로 포함/제외된다.

---

## 테스트 환경

| Item           | Value                                                  |
|----------------|--------------------------------------------------------|
| platform       | Local Docker (single node)                             |
| opensearch     | 2.11.1                                                 |
| jvm heap       | 6GB                                                    |
| indices        | 4 (PNU hash partition)                                 |
| shards         | 4/index (16 total)                                     |
| replicas       | 0                                                      |
| total docs     | 87,723,475                                             |
| doc structure  | nested (buildings, trades) + land object + geo_point   |

## 비교 대상

| Item            | lnbtpu (forcemerged) | lnbtpunf (no forcemerge) |
|-----------------|----------------------|--------------------------|
| segments/shard  | **1**                | **16~22**                |
| index pattern   | lnbtpu_*             | lnbtpunf_*               |
| data            | identical            | identical                | 동일 PNU 해시 분배
| store size      | ~8GB                 | ~8GB                     |

## 100회 단위 구간별 평균 (ms)

| Range      | Forcemerged | No Forcemerge |
|------------|-------------|---------------|
| 1~100      | 35          | 62            |
| 101~200    | 17          | 52            |
| 201~300    | 7           | 17            |
| 301~400    | 12          | 45            |
| 401~500    | 13          | 36            |
| 501~600    | 18          | 39            |
| 601~700    | 13          | 43            |
| 701~800    | 14          | 22            |
| 801~900    | 17          | 34            |
| 901~1000   | 13          | 46            |
| 1001~1100  | 17          | 40            |
| 1101~1200  | 8           | 28            |
| 1201~1300  | 18          | 40            |
| 1301~1400  | 12          | 22            |
| 1401~1500  | 13          | 21            |
| 1501~1600  | 17          | 32            |
| 1601~1700  | 18          | 31            |
| 1701~1800  | 16          | 46            |
| 1801~1900  | 13          | 23            |
| 1901~2000  | 21          | 33            |

## 재현 방법

```bash
# forcemerge O
python3 FORCEMERGE_BENCH.py --url http://localhost:9200 --index "lnbtpu_*" --tag lnbtpu_forcemerged --rounds 2000

# forcemerge X
python3 FORCEMERGE_BENCH.py --url http://localhost:9200 --index "lnbtpunf_*" --tag lnbtpunf_noforcemerge --rounds 2000
```

양쪽 동일한 필터 조합이 동일 순서로 생성되어 완전히 같은 조건으로 비교된다.