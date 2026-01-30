# Nested Object vs Flattened Primitive: 배열 데이터 저장 방식 비교

## 한눈에 이해하기

> **"필터에 직업으로 `의사` 선택, 사는 곳으로 `서울` 선택"**
- 서울에 살면서 의사인 것인지
- 혹은
- 서울에만 살아도 되고 또는 의사이기만 해도 되고

- **Nested Object**: 서울 사는 의사가 진짜 있어야 소개해줌
- **Flattened Primitive**: 부산 의사 + 서울 회사원 있어도 "있어요!" (크로스매치)



## 기술적 비교

### 저장 구조

**Nested Object 방식**

```json
{
  "pnu": "PNU-001",
  "buildings": [
    {
      "type": "nested",
      "doc": {
        "purpose": "단독주택",
        "area": 80
      }
    },
    {
      "type": "nested",
      "doc": {
        "purpose": "근린생활시설",
        "area": 300
      }
    }
  ]
}
```

- 내부적으로 각 nested object를 **별도의 hidden document**로 저장
- 문서 수: 부모 1 + nested object N개 = 총 N+1개

**Flattened Primitive 방식**

```json
{
  "pnu": "PNU-001",
  "buildings.purpose": [
    "단독주택",
    "근린생활시설"
  ],
  "buildings.area": [
    80,
    300
  ]
}
```

- 배열을 **평탄화(flatten)**하여 저장
- 문서 수: 부모 1개만

---

### 쿼리 방식

**Nested Query**

```json
{
  "nested": {
    "path": "buildings",
    "query": {
      "bool": {
        "filter": [
          {
            "term": {
              "buildings.purpose": "단독주택"
            }
          },
          {
            "range": {
              "buildings.area": {
                "gte": 200
              }
            }
          }
        ]
      }
    }
  }
}
```

- `bool.filter` 내 조건들이 **같은 nested object**에서 AND로 평가

**Object Query (일반 bool)**

```json
{
  "bool": {
    "filter": [
      {
        "term": {
          "buildings.purpose": "단독주택"
        }
      },
      {
        "range": {
          "buildings.area": {
            "gte": 200
          }
        }
      }
    ]
  }
}
```

- 각 조건이 **배열 전체**에서 독립적으로 평가 (OR 느낌)

---

### 성능 비교

| Metric       | Nested Object | Flattened Primitive |
|--------------|---------------|---------------------|
| Index Size   | 1.5x ~ 3x     | 1x (Base)           | nested object 수에 비례
| Index Speed  | 0.3x ~ 0.5x   | 1x (Base)           | hidden doc 생성 오버헤드
| Query Speed  | 0.1x ~ 0.3x   | 1x (Base)           | join 연산 필요
| Memory Usage | 2x ~ 4x       | 1x (Base)           | block join cache
| Accuracy     | 100%          | cross-match         | 정확도 vs 속도 트레이드오프

**실측 예시 (100만 필지, 평균 건물 3개/필지)**

| Operation      | Nested Object | Flattened Primitive |
|----------------|---------------|---------------------|
| Bulk Indexing  | ~45min        | ~15min              | 3배 차이
| Simple Query   | ~800ms        | ~150ms              | 5배 차이
| Complex Filter | ~2500ms       | ~300ms              | 8배 차이
| Index Size     | ~25GB         | ~12GB               | 2배 차이

---

## 선택 가이드

### Nested Object를 선택해야 할 때

- 필터 조건이 **같은 항목 내에서 AND**로 동작해야 할 때
- 검색 정확도가 **비즈니스 요구사항**일 때
- nested object 수가 **적을 때** (평균 10개 이하)

### Flattened Primitive를 선택해야 할 때

- "해당 속성을 가진 항목이 **하나라도 있으면**" 조건일 때
- 성능이 **최우선**일 때
- 크로스매치가 **허용 가능**할 때

### 하이브리드 전략

```
인덱스 2개 운영:
├── LNBTP (Nested Object): 정밀 필터링용, 상세 조회
└── LNBTP_FAST (Flattened Primitive): 대시보드 집계용, 대략적 카운트
```

---

## 현재 프로젝트 적용 현황

| Index | buildings | trades |
|-------|-----------|--------|
| LNBTP | nested    | nested | 정확한 AND 필터 필요
| LNBT  | nested    | nested | 정확한 AND 필터 필요
| LNBP  | nested    | -      | 건물 필터만, 실거래 없음
| LNB   | nested    | -      | 건물 필터만, 실거래 없음

**현재 구현**:

- 모든 building/trade 필터는 **단일 nested query 내 bool filter**로 처리
- 따라서 "단독주택 AND 100m2 이상"은 **같은 건물**에서 만족해야 함 ✅

---

## 참고 자료

- [Elasticsearch Nested Field Type](https://www.elastic.co/guide/en/elasticsearch/reference/current/nested.html)
- [OpenSearch Nested Query](https://docs.opensearch.org/latest/query-dsl/joining/nested/)
- [BMC - Nested vs Object](https://www.bmc.com/blogs/elasticsearch-nested-searches-embedded-documents/)