# 벤치마크 04: 인덱싱 테스트 (환경 무관 항목)

---

## 목적
- 로컬 싱글노드에서 측정해도 클러스터에 적용 가능한 인사이트 도출
- 절대값이 아닌 **상대적 비율/경향성** 검증

---

## 핵심 원칙

> "절대적 throughput(docs/sec)은 환경마다 다르지만,
> 상대적 비율과 경향성은 ES 엔진 특성이므로 환경과 무관하게 유효하다"

---

## 테스트 가능 항목

### 1. 데이터 구조별 인덱싱 비용 비율

| 비교 | 측정 | 클러스터 적용 가능성 |
|------|------|---------------------|
| geo_shape vs geo_point | 인덱싱 시간 비율 | ✅ 구조적 특성 |
| nested vs object | 인덱싱 시간 비율 | ✅ ES 엔진 특성 |
| nested 1depth vs 2depth | 오버헤드 증가율 | ✅ nested 처리 비용 |

**증명 가능한 것**: "geo_shape가 geo_point보다 X% 느리다"

### 2. refresh_interval 효과

| 설정 | 의미 |
|------|------|
| 1s (기본) | 실시간 검색 가능 |
| 30s | 배치 인덱싱 최적화 |
| -1 | refresh 비활성 (인덱싱 완료 후 수동) |

**증명 가능한 것**: "refresh_interval -1이 1s 대비 X% 빠르다"

### 3. bulk size 효율 곡선

| bulk size | 예상 |
|-----------|------|
| 100 | 오버헤드 큼 |
| 500 | 적정 |
| 1000 | 적정~최적 |
| 2000 | 메모리 부담 시작? |
| 5000 | 과다 |

**증명 가능한 것**: "최적 bulk size 구간은 500~1000" (피크 지점은 환경별 차이)

### 4. 필드 수/복잡도 영향

| 비교 | 의미 |
|------|------|
| 필드 적은 인덱스 vs 많은 인덱스 | 매핑 복잡도 비용 |
| keyword vs text | 분석기 비용 |

---

## 증명 불가 항목 (참고용)

| 항목 | 이유 |
|------|------|
| 절대 throughput | CPU, 메모리, 디스크 I/O 의존 |
| 동시 인덱싱 성능 | 노드 수, 네트워크 의존 |
| 클러스터 분산 효율 | 노드 구성 의존 |

---

## 전제 조건

- [ ] 동일한 데이터셋으로 비교 (문서 내용 동일)
- [ ] 측정 전 ES 재시작 또는 캐시 클리어
- [ ] 반복 측정으로 평균값 사용

---

## 테스트 설계

### 방법 1: 기존 데이터 활용
- PostgreSQL에서 동일 데이터를 다른 인덱스로 인덱싱
- 시간 측정

### 방법 2: 샘플 데이터
- 특정 범위(예: 서울시)만 추출
- 여러 조건으로 반복 인덱싱

---

## 측정 방식

```
시작 시간 기록
인덱싱 실행
종료 시간 기록
소요 시간 = 종료 - 시작
throughput = 문서 수 / 소요 시간
```

비율 계산:
```
비율 = (조건A throughput) / (조건B throughput)
```

---

## 미결정 사항

1. 어떤 비교 항목을 우선할지
   - [ ] geo_shape vs geo_point
   - [ ] nested vs object
   - [ ] refresh_interval
   - [ ] bulk size

2. 데이터 범위 (전체? 샘플?)

3. 반복 횟수

---

## 예상 결과 가설

| 항목 | 예상 |
|------|------|
| geo_shape vs geo_point | shape가 30~50% 느릴 것 |
| nested vs object | nested가 10~30% 느릴 것 |
| refresh -1 vs 1s | -1이 20~40% 빠를 것 |
| bulk 1000 vs 100 | 1000이 50%+ 빠를 것 |

---

## 결과 활용

로컬에서 검증된 비율/경향성을 문서화하여:
1. 클러스터 설정 시 참고 자료로 활용
2. 인덱스 설계 시 구조 선택 근거 제공
3. 배치 인덱싱 파라미터 튜닝 가이드

---

## 대량 데이터 인덱싱 가이드

### Reference

- [Tune for indexing speed](https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-indexing-speed.html)
- [Update Index Settings API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-update-settings.html)

### Step 1. Pre-indexing: Optimize Settings

쓰기 성능 극대화를 위해 검색 가시성과 복제를 비활성화

```
PUT /{index_name}/_settings
```

```json
{
  "index": {
    "refresh_interval": "-1",
    "number_of_replicas": 0
  }
}
```

| Setting | Value | Description |
|---------|-------|-------------|
| refresh_interval | -1 | refresh 비활성 (검색 불가) |
| number_of_replicas | 0 | 복제 비활성 (쓰기만 집중) |

### Step 2. Bulk Indexing

```
POST /_bulk
POST /{index_name}/_bulk
```

```text
{ "index" : { "_index" : "test_index" } }
{ "field1" : "value1" }
{ "index" : { "_index" : "test_index" } }
{ "field2" : "value2" }
```

### Step 3. Post-indexing: Restore Settings

`POST _refresh`는 일회성. 반드시 설정을 복구해야 자동 refresh 재개.

```
PUT /{index_name}/_settings
```

```json
{
  "index": {
    "refresh_interval": "1s",
    "number_of_replicas": 1
  }
}
```

> `refresh_interval: null` → 클러스터 기본값으로 복구

### Step 4. (Optional) Force Merge

검색 성능까지 챙기려면 세그먼트 병합.

```
POST /{index_name}/_forcemerge?max_num_segments=1
```

### Summary

| Step | API | Purpose |
|------|-----|---------|
| 1 | `PUT /_settings` | refresh/replica 비활성 |
| 2 | `POST /_bulk` | 대량 데이터 삽입 |
| 3 | `PUT /_settings` | refresh/replica 복구 |
| 4 | `POST /_forcemerge` | 세그먼트 최적화 (선택) |

---

## 실행 전 체크리스트

- [ ] 테스트 항목 우선순위 결정
- [ ] 데이터 범위 결정
- [ ] 테스트 인덱스 명명 규칙
- [ ] 측정 스크립트 준비
