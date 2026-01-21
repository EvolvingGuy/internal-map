# Markers API

## 개요
- 필지 단위 마커 표시 API
- land_compact + registration 데이터 조합
- 줌 레벨이 매우 가까울 때만 사용 (필지별 표시)
- type 1, type 2 두 가지 API 제공, 페이지는 하나

---

## 인덱스 구조

### land_compact (기존)
```
LandCompactDocument:
  - pnu: keyword
  - sd, sgg, emd: keyword
  - land:
    - center: geo_point
    - jiyukCd1, jimokCd, area, price
  - building: (nullable)
  - lastRealEstateTrade: (nullable)
```

### registration (신규)
```
RegistrationDocument:
  - id: integer
  - pnuId: keyword
  - sidoCode, sggCode, umdCode, riCode: keyword
  - registrationType, registrationProcess: keyword
  - property: keyword
  - address, roadAddress: text
  - center: geo_point
  - geometry: geo_shape
  - createdAt: date
  - userId: long
  - isLatest, isExpunged, ...: boolean
```

---

## API 상세

### Type 1: Land 우선 조회
- 경로: `GET /api/markers/type1`
- 용도: 토지/건물 기준으로 등기 현황 조회

#### 처리 순서
1. bbox 내 ES land_compact 조회 (LcAggFilter 적용)
2. 조회된 pnu 목록으로 ES registration aggregation
3. land_compact와 registration 둘 다 존재하는 pnu만 응답

### Type 2: Registration 우선 조회
- 경로: `GET /api/markers/type2`
- 용도: 등기 기준으로 토지/건물 현황 조회

#### 처리 순서
1. bbox 내 ES registration 조회 → pnu_id set 추출
2. pnu_id 목록으로 ES land_compact 조회
3. land_compact와 registration 둘 다 존재하는 pnu만 응답

---

## 요청 파라미터

### 공통 (bbox)
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| swLng | Double | Y | 남서 경도 |
| swLat | Double | Y | 남서 위도 |
| neLng | Double | Y | 북동 경도 |
| neLat | Double | Y | 북동 위도 |

### Registration 필터
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| userId | Long | N | 내 등기 집계용 |
| minCreatedDate | LocalDate | N | 등기 생성일 시작 (gte) |
| maxCreatedDate | LocalDate | N | 등기 생성일 종료 (lte) |

### Land Compact 필터 (LcAggFilter)
기존 LC aggregation 필터 전체 지원

#### Building Section
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| buildingMainPurpsCdNm | String (comma) | 주용도 (단독주택,공동주택,...) |
| buildingRegstrGbCdNm | String (comma) | 일반,집합 |
| buildingPmsDayRecent5y | Boolean | 허가일 최근 5년 |
| buildingStcnsDayRecent5y | Boolean | 착공일 최근 5년 |
| buildingUseAprDayStart | Int | 준공연도 시작 |
| buildingUseAprDayEnd | Int | 준공연도 종료 |
| buildingTotAreaMin/Max | BigDecimal | 연면적 범위 |
| buildingPlatAreaMin/Max | BigDecimal | 대지면적 범위 |
| buildingArchAreaMin/Max | BigDecimal | 건축면적 범위 |

#### Land Section
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| landJiyukCd1 | String (comma) | 용도지역 코드 |
| landJimokCd | String (comma) | 지목 코드 |
| landAreaMin/Max | Double | 토지면적 범위 |
| landPriceMin/Max | Long | 공시지가 범위 |

#### Trade Section
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| tradeProperty | String (comma) | 거래 유형 |
| tradeContractDateStart/End | LocalDate | 거래일 범위 |
| tradeEffectiveAmountMin/Max | Long | 거래금액 범위 |
| tradeBuildingAmountPerM2Min/Max | BigDecimal | 건물 평당가 범위 |
| tradeLandAmountPerM2Min/Max | BigDecimal | 토지 평당가 범위 |

---

## 응답 모델

### 전체 응답
```json
{
  "totalCount": 150,
  "elapsedMs": 234,
  "items": [ MarkerItem, ... ]
}
```

### MarkerItem
```json
{
  "pnu": "1111010100100010001",
  "center": { "lat": 37.5665, "lon": 126.978 },
  "land": {
    "jiyukCd1": "14",
    "jimokCd": "08",
    "area": 165.5,
    "price": 12500000
  },
  "building": {
    "mgmBldrgstPk": "11680-100017448",
    "mainPurpsCdNm": "단독주택",
    "regstrGbCdNm": "일반",
    "useAprDay": "2015-03-20",
    "totArea": 285.5
  },
  "lastRealEstateTrade": {
    "property": "SINGLE",
    "contractDate": "2024-06-15",
    "effectiveAmount": 450000000
  },
  "registration": {
    "count": 5,
    "lastAt": "2025-01-15T10:30:00",
    "myCount": 2,
    "myLastAt": "2025-01-10T14:20:00"
  }
}
```

### registration 집계 상세
| 필드 | 타입 | 설명 |
|------|------|------|
| count | Int | 해당 pnu의 전체 등기 수 |
| lastAt | DateTime | 해당 pnu의 가장 최근 등기 createdAt |
| myCount | Int | 해당 pnu의 내 등기 수 (userId 매칭) |
| myLastAt | DateTime | 해당 pnu의 내 가장 최근 등기 createdAt |

---

## ES Aggregation 전략 (Registration)

pnu_id별로 count, lastAt, myCount, myLastAt 집계

```
POST /registration/_search
{
  "size": 0,
  "query": {
    "bool": {
      "must": [
        { "terms": { "pnuId": ["pnu1", "pnu2", ...] } }
      ],
      "filter": [
        { "range": { "createdAt": { "gte": "...", "lte": "..." } } }
      ]
    }
  },
  "aggs": {
    "by_pnu": {
      "terms": { "field": "pnuId", "size": 10000 },
      "aggs": {
        "lastAt": { "max": { "field": "createdAt" } },
        "my_filter": {
          "filter": { "term": { "userId": 123 } },
          "aggs": {
            "myLastAt": { "max": { "field": "createdAt" } }
          }
        }
      }
    }
  }
}
```

---

## 페이지

### 경로
- `/page/markers`

### 기능
- 네이버 지도 표시
- 줌 레벨 17 이상에서만 활성화
- 지도 이동 시 type1, type2 API 동시 호출
- 마커 색상/크기로 type 구분
  - type1: 파란색, 큰 마커
  - type2: 주황색, 작은 마커
- info panel 표시
  - type1 마커 수, 응답 시간
  - type2 마커 수, 응답 시간
  - 총 필지 수

### 마커 클릭 시
- 해당 필지 상세 정보 풍선 표시
- land, building, trade, registration 정보 모두 표시

---

## 실행 계획

### 1단계: DTO 정의
- `MarkerRequest`: bbox + 모든 필터
- `MarkerResponse`: 응답 DTO
- `MarkerItem`: 개별 마커 데이터
- `RegistrationAgg`: 등기 집계 데이터

### 2단계: RegistrationAggService
- pnu_id 목록으로 registration aggregation
- terms agg + sub-agg 로직

### 3단계: MarkerService
- `getMarkersType1(request)`: land_compact 우선
- `getMarkersType2(request)`: registration 우선
- 교집합 머지 로직

### 4단계: MarkerController
- `GET /api/markers/type1`
- `GET /api/markers/type2`
- `GET /page/markers`

### 5단계: Freemarker 페이지
- `templates/es/markers.ftl`
- 두 API 동시 호출 및 마커 표시
