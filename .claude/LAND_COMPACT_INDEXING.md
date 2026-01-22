# Land Compact 인덱싱 가이드

## 개요
`land_compact` 인덱스는 필지 단위 데이터를 Elasticsearch에 저장하여 Marker API에서 빠른 조회를 지원한다.

## 인덱스 구조

```
land_compact
├── pnu (keyword)           - PNU 코드 19자리
├── sd (keyword)            - 시도 코드 2자리
├── sgg (keyword)           - 시군구 코드 5자리
├── emd (keyword)           - 읍면동 코드 8자리
├── land
│   ├── jiyukCd1 (keyword)  - 용도지역 코드
│   ├── jimokCd (keyword)   - 지목 코드
│   ├── area (double)       - 토지면적
│   ├── price (long)        - 개별공시지가
│   ├── center (geo_point)  - 중심 좌표
│   └── geometry (keyword)  - GeoJSON 문자열 (렌더링용)
├── building
│   ├── mgmBldrgstPk        - 건축물대장키
│   ├── mainPurpsCdNm       - 주용도
│   ├── regstrGbCdNm        - 일반/집합
│   ├── pmsDay (date)       - 허가일
│   ├── stcnsDay (date)     - 착공일
│   ├── useAprDay (date)    - 준공일
│   ├── totArea             - 연면적
│   ├── platArea            - 대지면적
│   └── archArea            - 건축면적
└── lastRealEstateTrade
    ├── property            - 거래 구분
    ├── contractDate        - 계약일
    ├── effectiveAmount     - 거래가
    ├── buildingAmountPerM2 - 건물 단가
    └── landAmountPerM2     - 토지 단가
```

## 인덱싱 프로세스

### 병렬 처리 구조
- 워커 수: 20개
- 배치 크기: 3,000건
- 파티셔닝: SGG(시군구) 기반

### 파티셔닝 전략 비교

**1. SD (시도) 기반** - 초기 방식
- 워커 수: 최대 17개 (시도가 17개뿐)
- 분배: 불균형 (경기 800만 vs 세종 5만)

**2. SGG (시군구) 기반** - 현재 방식
- 워커 수: 20개 전부 사용 가능
- 분배: 양호 (~250개 시군구를 20개 그룹으로)

**3. 균등 분할 (ROW_NUMBER)**
- 워커 수: N개 자유롭게 설정
- 분배: 완벽 균등
- 단점: 경계 계산이 느림 (B-tree 인덱스 한계)

### 데이터 소스
- `external_data.land_characteristic` - 토지 정보
- `building_ledger_outline_summaries` - 건축물 요약
- `building_ledger_outline` - 건축물 상세
- `real_estate_trade` - 실거래 정보

## Geometry 저장 방식

### 현황
geometry는 GeoJSON 문자열로 저장됨 (index: false, docValues: false)

```json
"geometry": "{\"type\":\"Polygon\",\"coordinates\":[[[128.19,36.79],...]]}"
```

### 문제점
API 응답 시 이스케이프 문자(`\"`)로 인한 용량 오버헤드 발생

### 해결 방안 비교

**1. API에서 파싱 후 객체 반환**
- 장점
  - ES 재인덱싱 불필요
  - 즉시 적용 가능
  - 응답 용량 감소
- 단점
  - 매 요청마다 JSON.parse 비용
  - 백엔드 CPU 부하

**2. ES에 object로 저장** (land_compact_geo)
- 장점
  - API 응답 시 변환 불필요
  - 백엔드 부하 없음
  - 응답 용량 감소
- 단점
  - 전체 재인덱싱 필요
  - ES 저장 용량 약간 증가

**3. 현재 유지 + Gzip**
- 장점
  - 코드 변경 없음
  - 압축률 높음 (70-80%)
- 단점
  - 클라이언트 해제 비용
  - 이스케이프 오버헤드 그대로

### 결론
재인덱싱이 부담되지 않으면 **2번(object 저장)** 추천. ES 저장과 API 응답 모두 깔끔해짐.

## API 엔드포인트

### land_compact (geometry를 String으로)
- `PUT /api/es/lc/reindex` - 전체 재인덱싱
- `GET /api/es/lc/count` - 인덱스 문서 수
- `DELETE /api/es/lc` - 인덱스 삭제
- `PUT /api/es/lc/forcemerge` - 세그먼트 병합

### land_compact_geo (geometry를 Object로)
- `PUT /api/es/lc-geo/reindex` - 전체 재인덱싱
- `GET /api/es/lc-geo/count` - 인덱스 문서 수
- `DELETE /api/es/lc-geo` - 인덱스 삭제

### Marker API
- `/api/markers/type1`, `/api/markers/type2` - land_compact 사용
- `/api/markers-geo/type1`, `/api/markers-geo/type2` - land_compact_geo 사용

### 비교 페이지
- `/page/markers` - land_compact (String geometry)
- `/page/markers-geo` - land_compact_geo (Object geometry)

## 관련 파일

- `LcIndexingService.kt` - 인덱싱 서비스
- `LandCompactDocument.kt` - 문서 구조
- `LandCompactQueryService.kt` - 조회 서비스
- `LandCharacteristicCursorRepository.kt` - 커서 기반 조회
