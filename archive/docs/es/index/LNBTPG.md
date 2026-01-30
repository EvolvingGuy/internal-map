# LNBTPG (Land Nested Building Trade Point Geometry)
LNBTP + geometry 바이너리 저장 인덱스 - 마커 API 전용

## 핵심 차이점: geometryBinary

### 왜 이렇게 했는가?

**문제**: geo_shape 타입은 검색/집계 성능에 큰 부하
- 역색인 생성 → 인덱싱 느림
- doc_values 저장 → 메모리 점유
- 복잡한 폴리곤 → 쿼리 시 CPU 부하

**해결**: 바이너리로 압축 저장, 조회 시에만 디코딩
```
JTS Geometry → WKB → gzip → Base64 → ES binary 필드
```

### 매핑 설정

```json
"geometryBinary": {
  "type": "binary",
  "doc_values": false
}
```

| 설정       | 값      |
|-----------|---------|
| type      | binary  | Base64 인코딩 바이너리
| doc_values | false   | 힙 메모리 미사용, 정렬/집계 불가
| index     | (false) | binary 타입 기본값, 검색 불가

### 저장/조회 특성

| 항목        | geo_shape | geometryBinary |
|------------|-----------|----------------|
| 검색       | O         | X              | 역색인 없음
| 정렬/집계   | O         | X              | doc_values 없음
| 메모리     | 점유       | 미점유          | 디스크에만 존재
| 조회       | O         | O              | _source에서 추출
| 용량       | 1x        | ~0.1x          | WKB + gzip 압축

---

## 용량 비교 (예상)

### 단일 필지 geometry 기준

| Format      | Size (avg) |
|-------------|------------|
| GeoJSON     | ~2,000 B   | 기준
| WKB         | ~400 B     | 5배 압축
| WKB + gzip  | ~150 B     | 13배 압축
| Base64      | ~200 B     | 최종 (10배 압축)

### 인덱스 전체 기준 (3,800만 필지)

| Index | geometry      | Size (예상)  |
|-------|---------------|-------------|
| LNBT  | geo_shape     | ~25 GB      | 검색/집계 가능
| LNBTP | 없음           | ~16 GB      | geometry 없음
| LNBTPG | binary (압축) | ~17 GB      | 조회 전용 geometry

> 실측 후 업데이트 예정

---

## 인덱스 정보

| key       | value                                   |
|-----------|-----------------------------------------|
| IndexName | `land_nested_building_trade_point_geo`  |
| Shards    | 4                                       |
| Replicas  | 0                                       |

### 문서 구조 (Document)

```json
{
  "pnu": "1111010100100010000",
  "sd": "11",
  "sgg": "11110",
  "emd": "11110101",
  "land": {
    "jiyukCd1": "14",
    "jimokCd": "08",
    "area": 330.5,
    "price": 5200000,
    "center": { "lat": 37.5665, "lon": 126.9780 }
  },
  "geometryBinary": "H4sIAAAAAAAAA6tWKkktLlGyUlAqS8wpTtVR...",
  "buildings": [
    { "mgmBldrgstPk": "11110-100012345", "mainPurpsCdNm": "단독주택", "totArea": 150.5 }
  ],
  "trades": [
    { "property": "LAND", "contractDate": "2024-01-15", "effectiveAmount": 500000000 }
  ]
}
```

### ES 매핑 정의 (LNBTP와 차이점만)

```json
{
  "geometryBinary": {
    "type": "binary",
    "doc_values": false
  }
}
```

> 나머지 필드(pnu, sd, sgg, emd, land, buildings, trades)는 LNBTP와 동일

---

## 사용 시나리오

### Marker API (Zoom 17+)

```
1. 클라이언트: bbox 요청 (zoom 17 이상)
2. 서버: land.center로 geo_bounding_box 쿼리
3. 서버: _source에서 geometryBinary 추출
4. 서버: Base64 → gzip 해제 → WKB → GeoJSON 변환
5. 클라이언트: 폴리곤 마커 렌더링
```

### 집계 API (기존 유지)

```
LNBTPG의 집계 API는 LNBTP와 동일하게 동작
geometryBinary는 집계에 영향 없음 (doc_values: false)
```

---

## API 엔드포인트

### 관리 API

| Method | Endpoint                     |
|--------|------------------------------|
| PUT    | `/api/es/lnbtpg/reindex`     | 전체 재인덱싱
| PUT    | `/api/es/lnbtpg/forcemerge`  | 세그먼트 병합
| GET    | `/api/es/lnbtpg/count`       | 인덱스 문서 수
| DELETE | `/api/es/lnbtpg`             | 인덱스 삭제

### 집계 API

| Method | Endpoint                   |
|--------|----------------------------|
| GET    | `/api/es/lnbtpg/agg/sd`    | 시도별 집계
| GET    | `/api/es/lnbtpg/agg/sgg`   | 시군구별 집계
| GET    | `/api/es/lnbtpg/agg/emd`   | 읍면동별 집계

### Marker API (신규)

| Method | Endpoint                    |
|--------|-----------------------------|
| GET    | `/api/es/lnbtpg/markers`    | bbox 내 필지 마커 조회 (geometry 포함)

**Marker API 파라미터**

| Param | Type   |
|-------|--------|
| swLng | Double | 남서 경도
| swLat | Double | 남서 위도
| neLng | Double | 북동 경도
| neLat | Double | 북동 위도
| limit | Int    | 최대 조회 수 (기본 500)

**응답 예시**

```json
{
  "count": 42,
  "markers": [
    {
      "pnu": "1111010100100010000",
      "center": { "lat": 37.5665, "lon": 126.9780 },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[126.977, 37.566], [126.978, 37.566], ...]]
      },
      "land": { "jiyukCd1": "14", "jimokCd": "08", "area": 330.5 },
      "buildings": [...],
      "trades": [...]
    }
  ]
}
```

---

## 인덱싱 방식

LNBTP와 동일한 EMD 단위 병렬 처리 + geometry 압축 추가

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
    └─ 신규 인덱스 생성 (geometryBinary 매핑 포함)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회
    ├─ trade 조회
    ├─ geometry → WKB → gzip → Base64 변환  ← 추가
    ├─ LnbtpgDocument 생성
    └─ ES bulk indexing
           ↓
[4] Forcemerge
           ↓
[5] 결과 반환
```

---

## 관련 파일

- Controller: `controller/rest/LnbtpgRestController.kt`, `controller/rest/LnbtpgAggRestController.kt`
- Indexing: `es/service/lnbtpg/LnbtpgIndexingService.kt`
- Aggregation: `es/service/lnbtpg/LnbtpgAggregationService.kt`
- Query: `es/service/lnbtpg/LnbtpgQueryService.kt` (Marker API)
- Document: `es/document/land/LnbtpgDocument.kt`
- Util: `util/GeometryCompressor.kt` (WKB + gzip + Base64)
- Docker: `docker-compose.lnbtpg.yml` (volume: `geo_poc_es_lnbtpg`)

---

## LNBTP vs LNBTPG 비교

| Feature         | LNBTP  | LNBTPG          |
|-----------------|--------|-----------------|
| geometry        | 없음    | binary (압축)    |
| 용도            | 집계    | 집계 + 마커      |
| 인덱스 용량     | ~16 GB | ~17 GB (예상)   |
| Marker API      | X      | O               |
| 검색 성능       | 빠름    | 동일 (geometry 미사용) |

---

## 실측 결과

> 인덱싱 완료 후 업데이트 예정

| Metric            | LNBTP | LNBTPG |
|-------------------|-------|--------|
| Index Size        | -     | -      |
| Indexing Time     | -     | -      |
| Marker Query Time | N/A   | -      |
