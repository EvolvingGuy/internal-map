# 지도 서비스 POC
- 한국 행정구역 및 필지 체계 베이스 API
- 데모 API로 추후 다른 시스템에 통합될 예정
- POC 검토 편의를 위한 임시적 프로젝트

## 가지고 있는 테이블
### 스키마
- manage
### 테이블
- 아래는 (bjdong_cd, h3_index) 페어에 대한 pnu의 카운팅, 중심 위경도 합산
- r3_pnu_agg_{region_level}_{h3_index_resolution}
- r3_pnu_agg_emd_11
- r3_pnu_agg_emd_10
- r3_pnu_agg_emd_09
- r3_pnu_agg_sgg_08
- r3_pnu_agg_sgg_07
- r3_pnu_agg_sd_06
- r3_pnu_agg_sd_05
- 아래는 고정형 행정구역 리전 카운트
- r3_pnu_agg_static_region
### 상수
- region level
  - SD, SGG, EMD

## 데이터 처리 방식
- Spring Data JPARepository 형식으로 멀티 IN 방식 조회
- Redis Template을 통한 멀티 GET, SET 캐시
- Redis TTL은 컨텐츠 별로 상이할 것임
- 주요 절차 원칙
  - Redis Cache 조회
    - 있으면 즉시 응답
    - 없으면 DB 조회
      - Redis Cache (일단은 동기적으로)
      - 응답
    - 캐시 TTL은 전체 고정 1day로 처리
## 제공하고자 하는 컨텐츠
- 요청 형식
  - 모든 요청은 아래와 같은 클라이언트의 좌표를 필수로 포함
    - val swLng: Double,
    - val swLat: Double,
    - val neLng: Double,
    - val neLat: Double
- 필요한 페이지와 호출할 API
  - 테이블 그대로 조회 페이지
    - 경로 및 API 규칙
      - page) /page/pnu/agg/emd_{h3_index_res}
      - api)  /api/pnu/agg/emd_{h3_index_res}
    - 대상 테이블
      - r3_pnu_agg_emd_11
      - r3_pnu_agg_emd_10
      - r3_pnu_agg_emd_09
      - r3_pnu_agg_sgg_08
      - r3_pnu_agg_sgg_07
      - r3_pnu_agg_sd_06
      - r3_pnu_agg_sd_05
    - JPA 엔티티 정책
      - 테이블에서 `r3_` 프리픽스 제거한 이름으로 나머지는 기본 컨벤션 클래스명
    - 캐시 키 정책
      - 엔티티 이름대로
    - API 로직 플로우
      - FE 요청에 따른 h3_index 구하기. 각 페이지에 맞는 res 사용
      - 각 h3_index로 redis multi get 있으면 바로 응답
      - miss 케이스 DB 조회. 캐시 비동기 set
      - 응답
  - FE 뷰포트에 따른 가변형 행정구역 그루핑
    - 경로 및 API 규칙
      - page) /page/pnu/agg/dynamic/{region_level}{h3_index_res}
      - api) /api/pnu/agg/dynamic/{region_level}{h3_index_res}
    - 대상 테이블
      - r3_pnu_agg_emd_11
      - r3_pnu_agg_emd_10
      - r3_pnu_agg_emd_09
      - r3_pnu_agg_sgg_08
      - r3_pnu_agg_sgg_07
      - r3_pnu_agg_sd_06
      - r3_pnu_agg_sd_05
    - API 로직 플로우 (테이블 그대로 조회하는 것과 캐시 레이어 공유함)
      - FE 요청에 따른 h3_index 구하기. 각 페이지에 맞는 res 사용
      - 각 h3_index로 redis multi get 있으면 바로 응답
      - miss 케이스 DB 조회. 캐시 비동기 set
      - 구해진 것 안에서 emd, sgg, sd 코드로 그룹바이하여 합산
      - 응답
  - FE 뷰포트에 따른 고정형 행정구역 그루핑
    - 경로 및 API 규칙
      - page) /page/pnu/agg/static/{region_level}
      - api) /api/pnu/agg/static/{region_level}
    - 대상 테이블
      - r3_pnu_agg_static_region
      - r3_pnu_agg_emd_09
      - r3_pnu_agg_sgg_07
      - r3_pnu_agg_sd_05
    - API 로직 플로우
  ┌─────────────────────────────────────────────────────────────────────┐
  │ 클라이언트 요청: bbox + regionLevel (emd/sgg/sd)                    │
  └─────────────────────────────────────────────────────────────────────┘
  │
  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │ STEP 1: regionLevel → H3 resolution 결정                            │
  │                                                                     │
  │   emd → res 9 (66m)                                                │
  │   sgg → res 7  (460m)                                               │
  │   sd  → res 5  (3km)                                                │
  └─────────────────────────────────────────────────────────────────────┘
  │
  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │ STEP 2: bbox → H3 indexes                                           │
  │                                                                     │
  │   예: 서울 bbox + res 9 → 500개 H3 셀                               │
  │   예: 서울 bbox + res 5  → 20개 H3 셀                                │
  └─────────────────────────────────────────────────────────────────────┘
  │
  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │ STEP 3: 캐시 조회 (H3 index 단위)                                    │
  │                                                                     │
  │   캐시 키: "region-fixed:{level}:{h3Index}"                         │
  │   캐시 값: List<(regionCode, cnt, centerLat, centerLng)>            │
  │                                                                     │
  │   히트 → 바로 사용                                                   │
  │   미스 → STEP 4로                                                    │
  └─────────────────────────────────────────────────────────────────────┘
  │ (캐시 미스)
  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │ STEP 4: DB 조회 (2단계)                                              │
  │                                                                     │
  │   4-1. r3_pnu_agg_{level}_{res} 에서 H3 → region_code 매핑          │
  │        SELECT DISTINCT bjdong_cd FROM r3_pnu_agg_emd_10             │
  │        WHERE h3_index IN (...)                                      │
  │                                                                     │
  │   4-2. r3_pnu_region_count 에서 전체 카운트 조회                     │
  │        SELECT * FROM r3_pnu_region_count                            │
  │        WHERE region_level = 'emd' AND region_code IN (...)          │
  │                                                                     │
  │   4-3. H3별로 그룹핑하여 캐시 저장                                    │
  └─────────────────────────────────────────────────────────────────────┘
  │
  ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │ STEP 5: 중복 제거 후 응답                                            │
  │                                                                     │
  │   여러 H3에 같은 읍면동이 걸쳐있으면 중복 제거                        │
  │   → 고유 region_code별 (cnt, center) 반환                           │
  └─────────────────────────────────────────────────────────────────────┘



### Table DDL
CREATE TABLE manage.r3_pnu_agg_emd_11
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_emd_10
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_emd_09
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_sgg_08
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_sgg_07
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_sd_06
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_sd_05
(
code     bigint           NOT NULL,
h3_index bigint           NOT NULL,
cnt      int              NOT NULL,
sum_lat  double precision NOT NULL,
sum_lng  double precision NOT NULL,
PRIMARY KEY (code, h3_index)
);

CREATE TABLE manage.r3_pnu_agg_static_region (
level text NOT NULL,
code bigint NOT NULL,
name text not NULL,
cnt int NOT NULL,
center_lat double precision NOT NULL,
center_lng double precision NOT NULL,
PRIMARY KEY (level, code)
);