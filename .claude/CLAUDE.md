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
- 아래는 행정구역 경계 정보 (10자리 법정동 코드)
- mart_data.boundary_region
### 상수
- region level
  - SD, SGG, EMD

## 데이터 처리 방식
- 범위 조회는 한국으로 한정 
- Spring Data JPARepository 형식으로 멀티 IN 방식 조회
- Redis Template을 통한 멀티 GET, SET 캐시
- Redis TTL은 컨텐츠 별로 상이할 것임
- 캐시 규칙
  - 결과가 없는 경우에도 없는 걸로 캐시
  - 예를들어 특정 h3 index에 카운트가 0 이면 0도 캐시
  - 리스트나 셋이 빈 경우 빈 것을 캐시
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
  - FE 요청 제한
    - 불필요 격자가 조회되는 것 방지
      - 대한민국 영토, 영해로 범위 제한. 잘리지 않게 조금은 넉넉히
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
  - FE 뷰포트 바둑판식 그리드 그루핑 (읍면동 res 사용)
    - 최종적으로 결정된 h3 Res table을 사용하는 형태가 될 것임. 현재는 emd_10,sgg_07,sd_05를 사용  
    - 경로 및 API 규칙
      - page) /page/pnu/agg/grid/{h3_index_res}
      - api) /api/pnu/agg/grid/{h3_index_res}
    - FE 요청 파라미터 추가
      - naver map zoom level
      - web viewport width & height
    - 대상 테이블 & 네이버 줌 레벨 매핑
      - r3_pnu_agg_emd_10 & naver map zoom level 16~22 와 매핑
      - r3_pnu_agg_sgg_07 & naver map zoom level 12~15 와 매핑
      - r3_pnu_agg_sd_05  & naver map zoom level 0~11 와 매핑
    - API 로직 플로우 (테이블 그대로 조회하는 것과 캐시 레이어 공유함)
      - FE 요청에 따른 비박스로부터 해상도 450px 단위로 바독판 그리드 생성, 분할
      - FE 요청에 따른 h3 index 구하여 데이터 조회 (캐시와 디비 오가는 거 동일한 정책으로)
      - 구해진 각 h3 index 데이터들의 중심점을 포함하는 바둑판 그리드에 누적
        - 중심점이 포함되는 h3 index 들의 위경도를 다시 합산하여 바둑판 클러스터 마커의 위치를 결정
  - FE 필지 카운트
    - 최종적으로 결정된 h3 Res table을 사용하는 형태가 될 것임. 현재는 emd_10,sgg_07,sd_05를 사용
    - 뷰포트에 해당하는 필지의 총 카운팅. h3를 사용한 시점부터 100% 정확도는 요구하지 않음
    - 경로 및 API 규칙
      - page) /page/pnu/agg/count/{h3_index_res}
      - api) /api/pnu/agg/count/{h3_index_res}
    - FE 요청 파라미터 추가
      - naver map zoom level
    - 대상 테이블 & 네이버 줌 레벨 매핑
      - r3_pnu_agg_emd_10 & naver map zoom level 16~22 와 매핑
      - r3_pnu_agg_sgg_07 & naver map zoom level 12~15 와 매핑
      - r3_pnu_agg_sd_05  & naver map zoom level 0~11 와 매핑
    - API 로직 플로우 (테이블 그대로 조회하는 것과 캐시 레이어 공유함)
      - FE 요청에 따른 h3 index 구하여 데이터 조회 (캐시와 디비 오가는 거 동일한 정책으로)
      - 구해진 것들의 sum 값만 합산하여 응답
    - FE 표시 형태
      - info 패널에 표시
  - FE 뷰포트에 따른 고정형 행정구역 그루핑
    - 뷰포트에 걸친 행정구역의 전체 필지 카운트를 표시 (H3 기반이 아닌 행정구역 전체 카운트)
    - 경로 및 API 규칙
      - page) /page/pnu/agg/static/{region_level}
      - api) /api/pnu/agg/static/{region_level}
    - region_level 파라미터
      - emd: 읍면동 단위
      - sgg: 시군구 단위
      - sd: 시도 단위
    - 대상 테이블
      - r3_pnu_agg_emd_10, r3_pnu_agg_sgg_07, r3_pnu_agg_sd_05 (H3 → region code 매핑용)
      - r3_pnu_agg_static_region (행정구역 전체 카운트 조회용)
    - API 로직 플로우
      - regionLevel → H3 resolution 결정 (emd→10, sgg→7, sd→5)
      - bbox → H3 indexes 폴리필
      - H3 index 단위로 캐시 조회 (캐시 키: "region-fixed:{level}:{h3Index}")
      - 캐시 미스 시 r3_pnu_agg_{level}_{res}에서 해당 H3에 걸친 region code 목록 조회
      - r3_pnu_agg_static_region에서 region code의 전체 카운트 조회
      - H3별로 그룹핑하여 캐시 저장
      - 여러 H3에 같은 행정구역이 걸쳐있으면 중복 제거 후 응답
    - FE 표시 형태
      - 행정구역 중심점에 카운트 마커 표시
  - 화면 중심점 행정구역 인디케이터
    - 뷰포트 중심점이 속한 행정구역을 "시도 > 시군구 > 읍면동" 형태로 표시
    - 경로 및 API 규칙
      - api) /api/pnu/agg/indicator
    - 대상 테이블
      - r3_pnu_agg_emd_10 (중심점 H3 → region code 매핑)
      - mart_data.boundary_region (region code → fullName 조회)
    - API 로직 플로우
      - bbox 중심점 계산: (swLat + neLat) / 2, (swLng + neLng) / 2
      - 중심점 → H3 resolution 10 인덱스 변환
      - emd_10 캐시에서 해당 H3의 region code 목록 조회
      - 중심점과 가장 가까운 region 선택 (거리 제곱 비교)
      - boundary_region 캐시에서 10자리 법정동 코드로 fullName 조회
      - fullName의 공백을 " > "로 치환하여 응답
    - 응답 형식
      - indicator: "서울특별시 > 중구 > 신당동"
      - regionCode: "1114010100" (10자리)
      - elapsedMs: 응답 시간
    - FE 표시 형태
      - 화면 상단 중앙에 반투명 검정 배경의 pill 형태 풍선으로 표시
      - Freemarker 공통 모듈: templates/pnu/common/indicator.ftl
      - 레거시 제외 모든 PNU 페이지에서 사용

### 화면 서클 방식
- 바둑판 그리드에만 해당 하는 규칙
  - 카운트에 따라 크고 작고의 비를 결정해줄 것
  - 다만 그 min, max를 적절히 넣어서 사용에 문제가 되지 않도록 설정

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

CREATE TABLE mart_data.boundary_region (
region_code text NULL,
region_english_name text NULL,
region_korean_name text NULL,
region_full_korean_name text NULL,
geom public.geometry NULL,
is_donut_polygon bool NULL,
center_geom public.geometry NULL,
center_lng float8 NULL,
center_lat float8 NULL,
area_paths jsonb NULL,
gubun text NULL
);
CREATE INDEX idx_boundary_region_geom ON mart_data.boundary_region USING gist (geom);
CREATE INDEX idx_boundary_region_region_code_gubun ON mart_data.boundary_region USING btree (region_code, gubun);
CREATE INDEX idx_boundary_region_sido ON mart_data.boundary_region USING btree ("left"(region_code, 2));
CREATE INDEX idx_boundary_region_sigungu ON mart_data.boundary_region USING btree ("left"(region_code, 5));

-- 소스 필지 테이블
CREATE TABLE external_data.land_characteristic (
pnu text NULL,
bjdong_cd text NULL,
bjdong_nm text NULL,
regstr_gb_cd text NULL,
regstr_gb text NULL,
jibun text NULL,
jimok_sign text NULL,
std_year text NULL,
std_month text NULL,
jimok_cd text NULL,
jimok text NULL,
area text NULL,
jiyuk_cd_1 text NULL,
jiyuk_1 text NULL,
jiyuk_cd_2 text NULL,
jiyuk_2 text NULL,
land_use_cd text NULL,
land_use text NULL,
height_cd text NULL,
height text NULL,
shape_cd text NULL,
shape text NULL,
road_cd text NULL,
road text NULL,
price text NULL,
crtn_day text NULL,
geometry public.geometry(geometry, 4326) NULL,
create_dt timestamptz NULL,
center public.geometry(point, 4326) NULL,
is_donut bool NULL
);
CREATE INDEX land_characteristic_bjdong_cd_index ON external_data.land_characteristic USING btree (bjdong_cd);
CREATE INDEX land_characteristic_geometry_index ON external_data.land_characteristic USING gist (geometry);
CREATE INDEX land_characteristic_pnu_index ON external_data.land_characteristic USING btree (pnu);