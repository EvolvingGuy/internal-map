# Land Compact (LC) ES 인덱스 기반 Aggregation

## 개요
- ES 인덱스: `land-compact`
- 개별 필지 단위 데이터 (약 4천만 건)
- 행정구역별 aggregation API 및 페이지 제공

## 인덱스 구조
```
LandCompactDocument:
  - pnu: String (keyword)
  - sd: String (keyword) - 시도 코드 2자리
  - sgg: String (keyword) - 시군구 코드 5자리
  - emd: String (keyword) - 읍면동 코드 10자리
  - land:
    - center: geo_point (lat, lon)
    - jiyukCd1, jimokCd, area, price
  - building: (nullable)
  - lastRealEstateTrade: (nullable)
```

---

## 요구사항: 행정구역별 Aggregation

### API
| 레벨 | 경로 | 그룹핑 필드 |
|------|------|-------------|
| 시도 | `/api/lc/agg/sd` | `sd` |
| 시군구 | `/api/lc/agg/sgg` | `sgg` |
| 읍면동 | `/api/lc/agg/emd` | `emd` |

### 요청 파라미터
```
swLng: Double - 남서 경도
swLat: Double - 남서 위도
neLng: Double - 북동 경도
neLat: Double - 북동 위도
```

### 응답 형식
```json
{
  "totalCount": 1234567,
  "regionCount": 17,
  "elapsedMs": 234,
  "regions": [
    {
      "code": "11",
      "name": "서울특별시",
      "count": 892341,
      "center": { "lat": 37.5, "lng": 127.0 }
    }
  ]
}
```
- `name`: 없으면 null (화면에서는 code만 표시)

### Aggregation 로직
1. `geo_bounding_box` 필터로 뷰포트 내 문서 필터링
2. `terms` aggregation으로 행정구역 그룹핑
3. `geo_centroid` sub-aggregation으로 중심점 계산
4. count는 doc_count 사용

### 페이지
| 레벨 | 경로 |
|------|------|
| 시도 | `/page/lc/agg/sd` |
| 시군구 | `/page/lc/agg/sgg` |
| 읍면동 | `/page/lc/agg/emd` |

### 페이지 기능
- 네이버 지도 표시
- 지도 이동/줌 변경 시 API 호출 (debounce 300ms)
- 각 행정구역 중심점에 마커 표시
- 마커 크기: count에 따라 동적 (상한/하한 설정)
- 왼쪽 상단 info panel:
  - 행정구역 수
  - 총 PNU 수
  - 응답시간 (ms)

### 마커 크기 규칙
- 최소 크기: 40px
- 최대 크기: 80px
- count 범위에 따라 선형 보간

---

## 실행 계획

### 1단계: DTO 정의
- `LcAggRequest`: bbox 파라미터
- `LcAggResponse`: 응답 DTO
- `LcAggRegion`: 개별 행정구역 데이터

### 2단계: Service 구현
- `LcAggregationService`
  - `aggregateBySd(bbox)`: 시도별 aggregation
  - `aggregateBySgg(bbox)`: 시군구별 aggregation
  - `aggregateByEmd(bbox)`: 읍면동별 aggregation
- ES Java Client로 aggregation 쿼리 실행

### 3단계: Controller 구현
- `LcAggregationController`
  - `GET /api/lc/agg/sd`
  - `GET /api/lc/agg/sgg`
  - `GET /api/lc/agg/emd`
  - `GET /page/lc/agg/sd`
  - `GET /page/lc/agg/sgg`
  - `GET /page/lc/agg/emd`

### 4단계: Freemarker 페이지
- `templates/es/lc/agg.ftl` (공통 템플릿)
- title, apiPath, level 변수로 재사용

---

## 향후 추가 예정
- 줌레벨에 따른 자동 레벨 전환
- 추가 필터 (building 유무, trade 유무 등)