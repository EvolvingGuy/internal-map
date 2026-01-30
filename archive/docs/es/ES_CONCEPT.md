# Elasticsearch 개념 정리

## 클러스터 구조

### Node (노드)
- ES 인스턴스 하나 = 노드 하나
- 역할별 분류: Master, Data, Coordinating, Ingest
- 싱글 노드 환경에서는 모든 역할 수행

### Shard (샤드)
- 인덱스를 수평 분할한 단위
- **Primary Shard**: 데이터 분산 저장 (전체 = Primary 샤드들의 합)
- 한 번 설정하면 변경 불가 (reindex 필요)
- 샤드 = Lucene 인덱스 1개
- 샤드 분할 자체가 병렬 검색 가능 → 읽기 성능 향상

### Replica (레플리카)
- 해당 Primary Shard의 완전 복제본
- 고가용성 (Primary 장애 시 대체)
- 읽기 부하 분산 (여러 노드에서 동시 읽기)
- 런타임에 변경 가능
- **같은 노드에 동일 번호의 Primary-Replica 배치 불가**

```
Cluster
├── Node 1 (Master + Data)
│   ├── Index A - Primary Shard 0
│   ├── Index A - Primary Shard 1
│   └── Index B - Replica Shard 0   ← Primary 0은 Node 2에
├── Node 2 (Data)
│   ├── Index A - Replica Shard 0   ← Primary 0은 Node 1에
│   ├── Index A - Replica Shard 1
│   └── Index B - Primary Shard 0
```

---

## 데이터 분배

### 샤드 라우팅
```
shard_num = hash(_routing) % number_of_shards
```
- `_routing` 기본값은 문서의 `_id`
- 샤드 수 변경 시 해시 결과 달라짐 → reindex 필요

### 예시 (문서 100개, 샤드 3개)
```
전체 데이터 100개
├── Primary Shard 0: 33개 (doc 0, 3, 6, 9...)
├── Primary Shard 1: 33개 (doc 1, 4, 7, 10...)
└── Primary Shard 2: 34개 (doc 2, 5, 8, 11...)

전체 데이터 = Shard 0 + Shard 1 + Shard 2
```

---

## 검색 흐름

```
검색 요청
    ↓
Coordinating Node
    ├→ Shard 0 검색 (병렬)
    ├→ Shard 1 검색 (병렬)
    └→ Shard 2 검색 (병렬)
    ↓
결과 병합 후 반환
```

- 샤드 많을수록 병렬 검색 가능
- 너무 많으면 병합 오버헤드 증가

---

## Segment & Forcemerge

### Segment
- 샤드 내부의 불변 데이터 단위
- 문서 추가/수정 시 새 세그먼트 생성
- 삭제는 마킹만 (물리 삭제 X)

### Forcemerge
- 여러 세그먼트 → 하나로 병합
- 삭제 마킹된 문서 물리적 제거
- 검색 성능 최적화

```
인덱싱 중: 세그먼트 다수 생성
    ↓
인덱싱 완료 후: forcemerge(1)
    ↓
단일 세그먼트 → 검색 최적화
```

**주의**: 쓰기 중 forcemerge 사용 시 성능 저하

---

## 싱글 노드 vs 클러스터

### 싱글 노드
```yaml
number_of_shards: N      # 병렬 검색용 (의미 있음)
number_of_replicas: 0    # 필수 (배치할 노드 없음)
```
- replicas > 0 설정 시: 클러스터 상태 yellow (unassigned)

### 클러스터
```yaml
number_of_shards: N      # 노드 수 고려
number_of_replicas: 1+   # 고가용성 + 읽기 분산
```

---

## Primary vs Replica 역할 비교

| Category            | Primary Split | Replica |
|---------------------|---------------|---------|
| Data Distribution   | O             | -       | 데이터 분산 저장
| Parallel Search     | O             | -       | 병렬 검색
| Read Load Balancing | -             | O       | 읽기 부하 분산
| High Availability   | -             | O       | 고가용성
| Runtime Change      | X (reindex)   | O       | 런타임 변경

---

## 참고 자료
- [Elasticsearch Sizing Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/size-your-shards.html)
- [Bulk API Best Practices](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html)