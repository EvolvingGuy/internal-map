# Elasticsearch 개발/운영 지침

## 샤드 수 기준

### 문서 수 기준
| Doc Count    | Shards |
|--------------|--------|
| ~100K        | 1      | 오버헤드 최소화
| 100K ~ 1M    | 1~2    | 싱글 노드면 1 권장
| 1M ~ 5M      | 2~3    | 병렬 처리 이점 시작
| 5M ~ 10M     | 3~5    | 검색/색인 병렬화
| 10M ~ 100M   | 5~10   | 노드 수 고려
| 100M+        | 10+    | 클러스터 규모에 따라

### 샤드 크기 기준
| Type | Size        |
|------|-------------|
| Rec  | 10GB ~ 50GB | 최적 성능 구간
| Max  | 50GB        | 초과 시 분할 권장
| Min  | 1GB         | 미만 시 오버헤드 비효율

### 계산 공식
```
샤드 수 = ceil(예상 데이터 크기 / 30GB)
```

### 프로젝트 인덱스별 설정
| Index                         | Doc Count   | Shards |
|-------------------------------|-------------|--------|
| `land_static_region_cluster`  | ~5,000      | 1      | 소규모, 오버헤드 최소화
| `land_dynamic_region_cluster` | ~5,000,000  | 3      | 대규모, 병렬 처리 필요
| `land_compact`                | ~36,000,000 | 5      | 대규모, 필터 쿼리 빈번
| `land_compact_geo`            | ~36,000,000 | 5      | geo_shape 쿼리 부하 분산

---

## Bulk Indexing 기준

### ES 권장 사항
| Item         | Recommended    | Max   |
|--------------|----------------|-------|
| Payload Size | 5MB ~ 15MB     | 100MB | 페이로드 크기
| Doc Count    | 1,000 ~ 10,000 | -     | 문서 수

### 문서 크기별 Bulk Size
| Avg Doc Size | Bulk Size | Payload |
|--------------|-----------|---------|
| ~100 bytes   | 10,000    | ~1MB    |
| ~500 bytes   | 5,000     | ~2.5MB  |
| ~1KB         | 5,000     | ~5MB    |
| ~5KB         | 2,000     | ~10MB   |
| ~10KB        | 1,000     | ~10MB   |

### 프로젝트 Bulk Size 설정

#### 클러스터 인덱스
| Index | Doc Size | Bulk Size | Payload | 비고 |
|-------|----------|-----------|---------|------|
| LSRC  | ~150B    | 2,000     | ~0.3MB  | 전체 ~5000개, 한번에 가능 |
| LDRC  | ~100B    | 10,000    | ~1MB    | 대량 처리 |

#### 필지 인덱스
| Index | Doc Size (avg) | Doc Size (max) | Bulk Size | Payload (avg) | Payload (max) |
|-------|----------------|----------------|-----------|---------------|---------------|
| LC    | ~600B          | ~2KB           | 2,000     | ~1.2MB        | ~4MB          |
| LNB   | ~800B          | ~3KB           | 2,000     | ~1.6MB        | ~6MB          |
| LNBT  | ~1KB           | ~5KB           | 500       | ~0.5MB        | ~2.5MB        |

#### 문서 크기 산정 근거

**공통 필드** (~80B)
- pnu, sd, sgg, emd: ~30B
- land (without geometry): ~50B

**land.geometry** (가변, 폴리곤 복잡도에 따라)
- 단순: ~200B
- 평균: ~400B
- 복잡: ~1.5KB

**building/buildings**
- 단일 건물: ~100B
- LC: 1개 (object)
- LNB/LNBT: N개 (nested array)
- 보수적 추정: 평균 2~3개, 최대 10~20개

**trade/trades**
- 단일 실거래: ~60B
- LC/LNB: 1개 (최신 1건, object)
- LNBT: M개 (nested array)
- 보수적 추정: 평균 2~3개, 최대 10개 (연도 제한 시)

#### Bulk Size 선정 이유
- ES 권장 payload: **5~15MB**
- 20 workers 동시 전송 고려 → 보수적으로 max 15MB 이하 유지
- 3,000건 기준 max payload ~15MB로 권장 범위 내 유지

#### 주의사항
- 5,000건으로 설정 시 LNB, LNBT 인덱싱에서 `No buffer space available` 에러 발생 가능
- nested array(buildings, trades)로 인해 문서 크기 편차가 크므로 보수적 설정 권장

---

## 인덱싱 최적화 절차

### 대량 인덱싱 시
```kotlin
// 1. refresh_interval 비활성화
esClient.indices().putSettings { s ->
    s.index(indexName)
        .settings { it.refreshInterval { t -> t.time("-1") } }
}

// 2. 벌크 인덱싱 수행
bulkIndex(docs)

// 3. refresh_interval 복원
esClient.indices().putSettings { s ->
    s.index(indexName)
        .settings { it.refreshInterval { t -> t.time("1s") } }
}

// 4. forcemerge (읽기 전용 인덱스)
esClient.indices().forcemerge { f ->
    f.index(indexName).maxNumSegments(1L)
}
```

### Forcemerge 사용 시점
```
인덱싱 완료 → refresh → forcemerge(1) → 서비스 투입
```

**주의사항:**
- 읽기 전용 인덱스에서만 사용
- I/O 부하 큼 → 트래픽 적은 시간에 실행
- 쓰기 중 사용 시 성능 저하

---

## 환경별 설정

### 개발/싱글 노드
```kotlin
esClient.indices().create { c ->
    c.index(indexName)
        .settings { s ->
            s.numberOfShards("N")
                .numberOfReplicas("0")  // 필수
        }
}
```

### 프로덕션/클러스터
```kotlin
esClient.indices().create { c ->
    c.index(indexName)
        .settings { s ->
            s.numberOfShards("N")
                .numberOfReplicas("1")  // 최소 1
        }
}
```

### 명령어