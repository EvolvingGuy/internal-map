# CPU 코어 수와 Cold 쿼리 성능

## 결론
cold 쿼리 성능은 메모리보다 **CPU 코어 수가 지배적**이다.
샤드별 병렬 검색이 핵심이므로, 코어가 많을수록 샤드를 동시에 처리할 수 있다.

## 비교 데이터
동일 인덱스(lnbtpu, forcemerged, 4idx x 4shard = 16 shards), 전국 SD agg, profile OFF 기준.
필터 조합 다양성은 부족하지만 cache clear 후 반복 측정하여 개별 요청의 cold 지연을 확인했다.

| Item       | Local Docker (single node) | AWS 3-node cluster |
|------------|----------------------------|--------------------|
| cpu        | 14 cores (Apple Silicon)   | 2 vCPU x 3 = 6    |
| ram        | 6GB heap                   | 8GB heap x 3      |
| cold avg   | ~400ms                     | ~700~865ms         | 1.7~2.0x 차이

메모리는 AWS가 3배 이상 많지만, cold 쿼리는 로컬이 1.7~2배 빠르다.
16개 샤드가 동시에 스캔을 돌리는 구조에서, 14코어는 거의 1:1로 병렬 처리가 가능하고
6 vCPU는 샤드 간 CPU 경합이 발생한다.

## 요약
- 메모리는 File System Cache와 heap 여유에 영향 (warm 쿼리에 유리)
- CPU는 샤드 병렬 스캔에 직접 영향 (cold 쿼리에 결정적)
- 샤드 수 > CPU 코어 수이면 경합 발생 → tail latency 증가
- **cold 성능을 올리려면 CPU 스케일업이 가장 직접적**