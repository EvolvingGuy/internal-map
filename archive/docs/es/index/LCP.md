# LCP (Land Compact Point)
필지 단위 인덱스 - LC의 경량 버전, geo_shape 대신 geo_point만 사용하여 용량 최소화

## 인덱스 정보
| key        | value                |
|------------|----------------------|
| IndexName  | `land_compact_point` |
| Shards     | 3                    |
| Replicas   | 0                    |

### 매핑
```json
{
  "pnu": "keyword",
  "sd": "keyword (eagerGlobalOrdinals)",
  "sgg": "keyword (eagerGlobalOrdinals)",
  "emd": "keyword (eagerGlobalOrdinals)",
  "land": {
    "jiyukCd1": "keyword",
    "jimokCd": "keyword",
    "area": "double",
    "price": "long",
    "center": "geo_point"
  },
  "building": {
    "mgmBldrgstPk": "keyword",
    "mainPurpsCdNm": "keyword",
    "regstrGbCdNm": "keyword",
    "pmsDay": "date",
    "stcnsDay": "date",
    "useAprDay": "date",
    "totArea": "scaled_float (100)",
    "platArea": "scaled_float (100)",
    "archArea": "scaled_float (100)"
  },
  "lastRealEstateTrade": {
    "property": "keyword",
    "contractDate": "date",
    "effectiveAmount": "long",
    "buildingAmountPerM2": "scaled_float (100)",
    "landAmountPerM2": "scaled_float (100)"
  }
}
```

## LC와의 차이점

| Feature       | LC (Land Compact)              | LCP (Land Compact Point)      |
|---------------|--------------------------------|-------------------------------|
| 공간 데이터    | `geometry` (geo_shape)         | 없음                           |
| 공간 쿼리      | geo_shape intersects           | geo_bounding_box              |
| 인덱스 용량    | ~19GB                          | ~12GB (약 37% 감소)            |
| 쿼리 속도      | 느림 (폴리곤 교차 계산)         | 빠름 (단순 범위 비교)          |
| 정확도        | 필지 폴리곤 기준 정확           | 중심점 기준 근사               |

### geo_bounding_box 쿼리
```kotlin
// LC: geo_shape intersects (폴리곤 교차)
bool.must { must ->
    must.geoShape { geo ->
        geo.field("land.geometry")
            .shape { shape ->
                shape.shape(JsonData.of(envelopeJson))
                    .relation(GeoShapeRelation.Intersects)
            }
    }
}

// LCP: geo_bounding_box (중심점 범위)
bool.must { must ->
    must.geoBoundingBox { geo ->
        geo.field("land.center")
            .boundingBox { bb ->
                bb.tlbr { tlbr ->
                    tlbr.topLeft { tl -> tl.latlon { ll -> ll.lat(bbox.neLat).lon(bbox.swLng) } }
                        .bottomRight { br -> br.latlon { ll -> ll.lat(bbox.swLat).lon(bbox.neLng) } }
                }
            }
    }
}
```

**장점:**
- 인덱스 용량 대폭 감소 (geo_shape 제거)
- 쿼리 속도 향상 (단순 범위 비교)
- 필터 없을 때 LSRC fallback으로 초고속 응답

**단점:**
- 뷰포트 경계에 걸친 필지 누락 가능 (중심점 기준이므로)
- 폴리곤 시각화 불가

## API 엔드포인트

### 관리 API
| Method | Endpoint                  |
|--------|---------------------------|
| PUT    | `/api/es/lcp/reindex`     | 전체 재인덱싱 (백그라운드)
| PUT    | `/api/es/lcp/forcemerge`  | 세그먼트 병합
| GET    | `/api/es/lcp/count`       | 인덱스 문서 수
| DELETE | `/api/es/lcp`             | 인덱스 삭제

### 집계 API
| Method | Endpoint                |
|--------|-------------------------|
| GET    | `/api/es/lcp/agg/sd`    | 시도별 집계
| GET    | `/api/es/lcp/agg/sgg`   | 시군구별 집계
| GET    | `/api/es/lcp/agg/emd`   | 읍면동별 집계

## 인덱싱 방식
LC와 동일한 EMD 단위 병렬 처리 방식 사용.

| Config     | Value     |
|------------|-----------|
| Worker     | 10        | EMD 라운드로빈 분배
| Fetch Size | 1,000     | JPA Stream 힌트
| Bulk Size  | 1,000     | ES bulk 단위
| Dispatcher | IO        | 코루틴 디스패처

### 처리 순서
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:3, replica:0, geometry 매핑 없음)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회 (1건)
    ├─ trade 조회 (최신 1건)
    ├─ LcpDocument 생성 (center 필수, geometry 없음)
    └─ ES bulk indexing
           ↓
[4] Forcemerge
           ↓
[5] 결과 반환
```

## 비즈니스 필터 (LcAggFilter)
LC와 동일한 필터 구조 사용. [LC.md](LC.md) 참조.

## 관련 파일
- Controller: `controller/rest/LcpRestController.kt`, `controller/rest/LcpAggRestController.kt`
- Web: `controller/web/LcpAggWebController.kt`
- Indexing: `es/service/LcpIndexingService.kt`
- Aggregation: `es/service/LcpAggregationService.kt`
- Document: `es/document/land/LcpDocument.kt`
- Template: `templates/es/lcp/agg.ftl`
- Docker: `docker-compose.lcp.yml` (volume: `geo_poc_es_lcp`)
