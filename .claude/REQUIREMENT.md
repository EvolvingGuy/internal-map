# 줌 관련
- 00~11 SIDO 고정형 클러스터
- 12~14 SGG 고정형 클러스터
- 15~16 EMD 가변형 클러스터 혹은 500개 이하 시에 마커 (표기는 점 방식)
- 17~ GRID 클러스터, 500개 이하 시에 마커 (표기는 폴리곤)

# 참고
## 인덱싱의 경우 코루틴 패러럴리즘 20개로 한정하여 처리함. 멀티스레드 용도로 넌블러킹 안 되는 것 참고


# 필요한 ES Index
## 필지 고정형 행정구역 클러스터 (Land Static Region Cluster. LSRC)
- 비즈니스 필터가 없는 경우 사용
- 사전에 행정구역별로 데이터를 저장해 놓음
- 좌표계 이동에 따라 행정구역별 데이터가 바뀌지 않음
### 소스 데이터
- RDB external_data.land_characteristic
- RDB mart_data.boundary_region
### 인덱스 모델
#### Index name
- LandStaticRegionCluster
#### Fields
- level (ENUM. SD, SGG, EMD NOT NULL)
- code (STRING NOT NULL. FIXED LENGTH 10. LEFT PAD BY 0)
- name (STRING NOT NULL)
- count (INT NOT NULL DEFAULT 0) land_characteristic의 COUNT
- center (GEO_POINT NOT NULL) land_characteristic의 SUM(center) 
### 인덱스 절차
- RDB external_data.land_characteristic -> ES INDEX 전환
  1. land_characteristic 페이지 사이즈 조회
  2. land_characteristic을 N등분하고 코루틴 N개가 자신의 범위 안에서 M개씩 pnu 커서 방식으로 land_characteristic 순회
  3. 각자 land_characteristic를 순회하며 애플리케이션 수준에서 행정구역별 인덱스 모델 유지하고 누적. MAP<REGION_CODE, ES MODEL>
     - 시도(SD) PNU 앞 2자리, 8자리 0패딩으로 KEY
     - 시군구(SGG) PNU 앞 5자리, 5자리 0패딩으로 KEY
     - 읍면동(EMD) PMU 앞 8자리, 2자리 0패딩으로 KEY
  4. 스레드 세이프하게 잘 관리하여 데이터가 손실되는 것을 방지
  5. mart_data.boundary_region에서 region_code가 00으로 끝나지 않은 것들만 region_code, region_korean_name 두 필드 조회하여 맵 구성
  6. 별도 루프로 누적된 Map에 name 할당
  7. 순회 종료 후 2천개씩 ES 배치 인덱스
  8. forcemerge(max_num_segments=1) 실행

## 가변형 행정구역 클러스터 (Land Dynamic Region Cluster. LDRC)
- 비즈니스 필터가 없는 경우 사용
- 사전에 행정구역 레벨에 따라 (행정구역, H3 Res)을 키로 데이터를 인덱싱해 놓음
- 좌표계 이동에 따라 행정구역별 데이터가 변동됨. 좌표 안에 들어온 (행정구역, H3 Res)를 찾고 합산하는 방식
### 소스 데이터
- RDB external_data.land_characterisitc
### 인덱스 모델
#### Index name
- LandDynamicRegionCluster
#### Fields (name, es type, kotlin type, description)s
- id (STRING NOT NULL. "{level}{code}{h3Index}")
- level (ENUM NOT NULL. SD, SGG, EMD)
- code (LONG NOT NULL. 읍면동 코드 10자리)
- h3Index (LONG NOT NULL. H3 셀 인덱스)
- count (INT. 필지 수)
- center (GEO_POINT NOT NULL) land_characteristic의 SUM(center)
### 인덱스 절차
- RDB manage.r3_pnu_agg_emd_10 -> ES INDEX 전환 (EMD -> SGG -> SD 체인 방식)
  1. r3_pnu_agg_emd_10 페이지 사이즈 조회
  2. r3_pnu_agg_emd_10을 10등분하고 코루틴 10개가 자신의 범위 안에서 2천건씩 (code, h3_index) 커서 방식으로 순회
  3. 각 코루틴이 가져온 즉시 ES 벌크 인덱싱 (EMD 레벨, H3 Res 10)
  4. EMD 인덱싱 완료 후, ES EMD 레벨 문서 순회 (search_after 방식, 5천건씩 페이징)
     - h3.cellToParent(h3Index, res=7)로 상위 H3 인덱스 변환
     - (상위 H3 인덱스, 읍면동 코드) 기준으로 애플리케이션 수준에서 집계. MAP<KEY, AGG_DATA>
     - count 합산, center 위경도 합산
  5. SGG 레벨(H3 Res 7) ES 벌크 인덱싱
  6. ES SGG 레벨 문서 순회 (search_after 방식, 5천건씩 페이징)
     - h3.cellToParent(h3Index, res=5)로 상위 H3 인덱스 변환
     - (상위 H3 인덱스, 시군구 코드) 기준으로 애플리케이션 수준에서 집계. MAP<KEY, AGG_DATA>
     - count 합산, center 위경도 합산
  7. SD 레벨(H3 Res 5) ES 벌크 인덱싱
  8. forcemerge(max_num_segments=1) 실행

## 클러스터 & 마커 필지 (Land Compact. LC)
- 비즈니스 필터가 있는 경우 사용
- 필지 단위로 인덱싱하여 필터링 및 집계 수행
- 줌 레벨에 따라 SD, SGG, EMD 단위로 terms aggregation되어 사용
### 소스 데이터
- RDB external_data.land_characteristic
- RDB external_data.building_ledger_outline_summaries
- RDB external_data.building_ledger_outline
- RDB external_data.r3_real_estate_trade
### 인덱스 모델
#### Index name
- LandCompact
#### Fields (name, es type, kotlin type, description)
- pnu (keyword, String not null. PNU 코드 19자리)
- sd (integer eagerGlobalOrdinals, Int not null. 시도 코드 2자리)
- sgg (integer eagerGlobalOrdinals, Int not null. 시군구 코드 5자리)
- emd (keyword eagerGlobalOrdinals, String not null. 읍면동 코드 8자리)
- land
  - land.jiyukCd1 (keyword, Enum<LandJiyuk1> nullable, 용도지역으로 값은 별도 기록)
  - land.jimokCd (keyword, Enum<LandJimok> nullable, 지목으로 값은 별도 기록)
  - land.area (double, Double nullable, 토지면적)
  - land.price (long, Long nullable, 개별공시지가)
  - land.center (geo_point, Map<String, Double> not null, 필지 중심 좌표)
- building
  - building.mgmBldrgstPk (keyword, String not null, 건축물대장키)
  - building.mainPurpsCdNm (keyword, String nullable, 주용도로 값은 별도 기록)
  - building.regstrGbCdNm (keyword, String nullable, 일반|집합|NULL)
  - building.pmsDay (date, LocalDate nullable, 허가/신고일)
  - building.stcnsDay (date, LocalDate nullable, 착공일)
  - building.useAprDay (date, LocalDate nullable, 준공연도)
  - building.totArea (scaled_float, BigDecimal nullable, 연면적)
  - building.platArea (scaled_float, BigDecimal nullable, 대지면적)
  - building.archArea (scaled_float, BigDecimal nullable, 건축면적)
- lastRealEstateTrade
  - lastRealEstateTrade.property (keyword, String not null, 구분)
  - lastRealEstateTrade.contractDate (date, LocalDate not null, 거래연월)
  - lastRealEstateTrade.effectiveAmount (long, Long not null, 거래가)
  - lastRealEstateTrade.buildingAmountPerM2 (scaled_float, BigDecimal nullable, 건물면적당단가)
  - lastRealEstateTrade.landAmountPerM2 (scaled_float, BigDecimal nullable, 토지면적당단가)
### 인덱스 절차
- RDB external_data.land_characteristic -> ES INDEX 전환
  1. land_characteristic 페이지 사이즈 조회
  2. land_characteristic을 10등분하고 코루틴 10개가 자신의 범위 안에서 3천건씩 pnu 커서 방식으로 순회
  3. pnu IN 방식으로 아래 RDB Table마다 JPA기반 데이터 적재 및 변환. 3천건씩 in절 적재후 맵으로 변환.
     - `external.building_ledger_outline_summaries` 토지의 pnu 11번째 자리 변환하여 조회. (1 -> 0, else -> 1)
     - `external.building_ledger_outline` 토지의 pnu 11번째 자리 변환하여 조회. (1 -> 0, else -> 1)
     - `manage.r3_real_estate_trade` 그대로 조회하되 pnu마다 max(id) 단 건으로 특정. groupBy 사용 권장. 별도 모델에 프로젝션 사용
  4. 각 ES모델마다 순회하며 building, lastRealEstateTrade 완성
     - building_ledger_outline_summaries 우선적으로 매핑 시도하고 mgmBldrgstPk이 비어있는 경우 building_ledger_outline 매핑
  5. 순회 종료 후 forcemerge(max_num_segments=1) 실행





# 필요한 API

## LC Aggregation Filter (NEW)
LC 3개 페이지 (sd, sgg, emd) 공통 필터 스펙

### UI 구성
- 위치: 우측 사이드바 (non-collapse, overflow-y: auto)
- 섹션 순서: Building -> Land -> Trade
- 필터 변경 시 즉시 적용 (debounce 300ms)
- Reset All 버튼: 사이드바 상단에 위치

### API 파라미터 바인딩
- List 타입: 콤마 구분 (ex: `?buildingMainPurpsCdNm=APARTMENT,FACTORY`)
- Range 타입: 별도 파라미터 (ex: `?buildingTotAreaMin=100&buildingTotAreaMax=500`)

### Kotlin Filter Class
- 클래스명: LcAggFilter
- 필드 네이밍: depth 제외, prefix로 구분 (ex: buildingArchArea)
- 필터 상태 판별:
  - `null` 또는 `emptyList()` → 필터 미적용 (전체 조회)
  - 값이 있으면 필터 적용
- `fun hasAnyFilter(): Boolean` → 하나라도 설정되어 있으면 true

### Building Section
| Field | Kotlin Type | ES Field | ES Type | UI Type | Values/Description |
|-------|-------------|----------|---------|---------|---------------------|
| buildingMainPurpsCdNm | List<String>? | building.mainPurpsCdNm | keyword | checkbox | 단독주택, 제2종근린생활시설, 제1종근린생활시설, 공동주택, 창고시설, 공장, 업무시설, 숙박시설, 판매시설, 노유자시설, 교육연구시설, 운동시설, 동물및식물관련시설, 위험물저장및처리시설, 자동차관련시설 |
| buildingRegstrGbCdNm | List<String>? | building.regstrGbCdNm | keyword | checkbox | 일반, 집합 |
| buildingPmsDayRecent5y | Boolean? | building.pmsDay | date | checkbox | true=filter recent 5 years |
| buildingStcnsDayRecent5y | Boolean? | building.stcnsDay | date | checkbox | true=filter recent 5 years |
| buildingUseAprDayStart | Int? | building.useAprDay | date | number | year (gte yyyy-01-01) |
| buildingUseAprDayEnd | Int? | building.useAprDay | date | number | year (lte yyyy-12-31) |
| buildingTotAreaMin | BigDecimal? | building.totArea | scaled_float | number | gte |
| buildingTotAreaMax | BigDecimal? | building.totArea | scaled_float | number | lte |
| buildingPlatAreaMin | BigDecimal? | building.platArea | scaled_float | number | gte |
| buildingPlatAreaMax | BigDecimal? | building.platArea | scaled_float | number | lte |
| buildingArchAreaMin | BigDecimal? | building.archArea | scaled_float | number | gte |
| buildingArchAreaMax | BigDecimal? | building.archArea | scaled_float | number | lte |

### Land Section
| Field | Kotlin Type | ES Field | ES Type | UI Type | Values/Description |
|-------|-------------|----------|---------|---------|---------------------|
| landJiyukCd1 | List<String>? | land.jiyukCd1 | keyword | checkbox | See LandJiyuk1 enum below |
| landJimokCd | List<String>? | land.jimokCd | keyword | checkbox | See LandJimok enum below |
| landAreaMin | Double? | land.area | double | number | gte |
| landAreaMax | Double? | land.area | double | number | lte |
| landPriceMin | Long? | land.price | long | number | gte |
| landPriceMax | Long? | land.price | long | number | lte |

### Trade Section
| Field | Kotlin Type | ES Field | ES Type | UI Type | Values/Description |
|-------|-------------|----------|---------|---------|---------------------|
| tradeProperty | List<String>? | lastRealEstateTrade.property | keyword | checkbox | APARTMENT, COMMERCIAL_BUILDING, FACTORY_WAREHOUSE, FACTORY_WAREHOUSE_MULTI, LAND, MULTI, OFFICETEL, SHOPPING_AND_OFFICE, SINGLE |
| tradeContractDateStart | LocalDate? | lastRealEstateTrade.contractDate | date | date | gte |
| tradeContractDateEnd | LocalDate? | lastRealEstateTrade.contractDate | date | date | lte |
| tradeEffectiveAmountMin | Long? | lastRealEstateTrade.effectiveAmount | long | number | gte |
| tradeEffectiveAmountMax | Long? | lastRealEstateTrade.effectiveAmount | long | number | lte |
| tradeBuildingAmountPerM2Min | BigDecimal? | lastRealEstateTrade.buildingAmountPerM2 | scaled_float | number | gte |
| tradeBuildingAmountPerM2Max | BigDecimal? | lastRealEstateTrade.buildingAmountPerM2 | scaled_float | number | lte |
| tradeLandAmountPerM2Min | BigDecimal? | lastRealEstateTrade.landAmountPerM2 | scaled_float | number | gte |
| tradeLandAmountPerM2Max | BigDecimal? | lastRealEstateTrade.landAmountPerM2 | scaled_float | number | lte |

### Building Main Purpose Values (Top 15, 한글 그대로 사용)
| Value | Label | Count |
|-------|-------|-------|
| 단독주택 | 단독주택 | 3,409,648 |
| 제2종근린생활시설 | 제2종근린 | 561,947 |
| 제1종근린생활시설 | 제1종근린 | 465,851 |
| 공동주택 | 공동주택 | 305,609 |
| 창고시설 | 창고시설 | 247,956 |
| 동물및식물관련시설 | 동식물시설 | 208,870 |
| 공장 | 공장 | 165,775 |
| 노유자시설 | 노유자시설 | 36,114 |
| 숙박시설 | 숙박시설 | 32,681 |
| 업무시설 | 업무시설 | 30,057 |
| 교육연구시설 | 교육연구 | 27,770 |
| 종교시설 | 종교시설 | 24,177 |
| 자동차관련시설 | 자동차시설 | 20,762 |
| 위험물저장및처리시설 | 위험물시설 | 17,675 |
| 문화및집회시설 | 문화집회 | 8,411 |

### Building Registry Type Values (한글 그대로 사용)
| Value | Label |
|-------|-------|
| 일반 | 일반 |
| 집합 | 집합 |

### Trade Property Values (영어 코드 그대로 사용)
| Value | Label | Count |
|-------|-------|-------|
| LAND | Land | 3,783,326 |
| SINGLE | Single | 932,362 |
| COMMERCIAL_BUILDING | Commercial Building | 250,477 |
| MULTI | Multi | 219,135 |
| FACTORY_WAREHOUSE | Factory/Warehouse | 84,992 |
| SHOPPING_AND_OFFICE | Shopping & Office | 64,598 |
| APARTMENT | Apartment | 23,897 |
| OFFICETEL | Officetel | 4,773 |
| FACTORY_WAREHOUSE_MULTI | Factory/Warehouse Multi | 2,089 |

### Land Jiyuk (Zone) Values (Top 10, 숫자 코드 사용)
| Code | Label | Count |
|------|-------|-------|
| 64 | 계획관리 | 9,767,919 |
| 71 | 농림 | 6,659,083 |
| 63 | 생산관리 | 3,348,197 |
| 62 | 보전관리 | 3,309,832 |
| 43 | 자연녹지 | 3,270,901 |
| 14 | 2종일반주거 | 2,674,084 |
| 13 | 1종일반주거 | 1,647,248 |
| 44 | 개발제한 | 1,134,206 |
| 22 | 일반상업 | 686,872 |
| 81 | 자연환경보전 | 676,008 |

### Land Jimok (Land Category) Values (Top 10, 숫자 코드 사용)
| Code | Label | Count |
|------|-------|-------|
| 02 | 답 | 7,576,801 |
| 08 | 대 | 7,479,076 |
| 01 | 전 | 6,978,516 |
| 05 | 임야 | 4,787,553 |
| 14 | 도로 | 4,514,624 |
| 18 | 구거 | 944,795 |
| 28 | 잡종지 | 571,676 |
| 17 | 하천 | 549,662 |
| 09 | 공장용지 | 316,873 |
| 03 | 과수원 | 276,831 |


## 필터 구분 (Legacy)
- 설정 필터 (위경도, 클러스터 구분)
- 비즈니스 필터 (설정 필터 이외로 비즈니스 관련 정보)
  - mainPurpsCdNm 체크박스로 IN절 방식
    - 가설건축물
    - 공공업무시설
    - 공공시설
    - 공공용시설
    - 공장
    - 공동주택
    - 관광숙박시설
    - 관광휴게시설
    - 교정및군사시설
    - 교정시설
    - 교육연구및복지시설
    - 교육연구시설
    - 교회
    - 국방
    - 군사시설
    - 근린생활시설
    - 기숙사
    - 기타공공시설
    - 기타공장
    - 기타동식물관련시설
    - 기타사무소
    - 기타운동시설
    - 기타일반숙박시설
    - 기타자원순환관련시설
    - 기타제1종근린생활시설
    - 기타제2종근린생활시설
    - 기타창고시설
    - 기도원
    - 다가구주택
    - 다세대주택
    - 다중주택
    - 단란주점
    - 단독주택
    - 당구장
    - 도축장
    - 동물및식물관련시설
    - 동사무소
    - 마을공동시설
    - 묘지관련시설
    - 문화및집회시설
    - 발전시설
    - 방송통신시설
    - 변전소
    - 부대시설
    - 분뇨.쓰레기처리시설
    - 분뇨처리시설
    - 사당
    - 사무소
    - 사찰
    - 사회복지시설
    - 사진관
    - 상점(소매점)
    - 성당
    - 소매시장
    - 소매점
    - 소방서
    - 수련시설
    - 수리점
    - 숙박시설
    - 시장
    - 아동관련시설
    - 아파트
    - 야영장시설
    - 양수장
    - 어린이집
    - 업무시설
    - 여관
    - 연구소
    - 연립주택
    - 온실
    - 우체국
    - 운수시설
    - 운동시설
    - 위락시설
    - 위험물저장및처리시설
    - 유치원
    - 유흥주점
    - 의료시설
    - 일반공장
    - 일반업무시설
    - 일반음식점
    - 일반숙박시설
    - 자원순환관련시설
    - 자동차관련시설
    - 장례시설
    - 제실
    - 정비공장
    - 정수장
    - 제1종근린생활시설
    - 제2종근린생활시설
    - 제조업소
    - 종교시설
    - 종교집회장
    - 주유소
    - 창고시설
    - 축사
    - 체육도장
    - 파출소
    - 판매및영업시설
    - 판매시설
    - 학원
    - 휴게소
    - 휴게음식점
    - 휴양콘도미니엄
    - 화장실
  - regstrGbCdNm 체크박스로 값은 일반, 집합 두 가지 IN 절 방식
    - 일반
    - 집합
  - pmsDay  체크박스로 체크 되는 경우에 각 값이 있는 것들 이내에서 현재 기준 5년 이내인 것
  - stcnsDay 체크박스로 체크 되는 경우에 각 값이 있는 것들 이내에서 현재 기준 5년 이내인 것
  - useAprDay (nullable Int, start ~ end) year 기반으로 start는 초월초일, end는 막월말일 형태로. 그냥 연도만 넣어도 되면 그렇게
  - totArea (nullable BigDecimal, start, end) goe, loe 각자
  - platArea (nullable BigDecimal, start, end) goe, loe 각자
  - archArea (nullable BigDecimal, start, end) goe, loe 각자

  - jiyukCd1 IN절 값은 하단 참고
  - jimokCd IN절 값은 하단 참고
  - area start, end
  - price start, end

  - property IN절
    - APARTMENT
    - COMMERCIAL_BUILDING
    - FACTORY_WAREHOUSE
    - FACTORY_WAREHOUSE_MULTI
    - LAND
    - MULTI
    - OFFICETEL
    - SHOPPING_AND_OFFICE
    - SINGLE
  - contractDate LocalDate start, end
  - effectiveAmount Long start, end
  - buildingAmountPerM2 BigDecimal start, end
  - landAmountPerM2 BigDecimal start, end


## API 목록
- 클러스터 API
  - 비즈니스 필터 X
    - 적절한 H3 Res 
    - 설정 필터 클러스터 SD 
    - 설정 필터 클러스터 SGG
    - 설정 필터 클러스터 EMD
  - 필터 O
    - 
- 카운트 API
  - 비즈니스 필터 X
    - 필터 O
- 마커 API




# 타입 관련
## LandJimok 지목
전	01
답	02
과수원	03
목장용지	04
임야	05
광천지	06
염전	07
대	08
공장용지	09
학교용지	10
주차장	11
주유소용지	12
창고용지	13
도로	14
철도용지	15
제방	16
하천	17
구거	18
유지	19
양어장	20
수도용지	21
공원	22
체육용지	23
유원지	24
종교용지	25
사적지	26
묘지	27
잡종지	28

## LandJiyuk1 지역
개발제한구역	44
계획관리지역	64
관리지역	61
근린상업지역	23
농림지역	71
보전관리지역	62
보전녹지지역	41
생산관리지역	63
생산녹지지역	42
용도미지정지역	51
유통상업지역	24
일반공업지역	32
일반상업지역	22
자연녹지지역	43
자연환경보전지역	81
전용공업지역	31
제1종일반주거지역	13
제1종전용주거지역	11
제2종일반주거지역	14
제2종전용주거지역	12
제3종일반주거지역	15
준공업지역	33
준주거지역	16
중심상업지역	21


    - 가설건축물
    - 공공업무시설
    - 공공시설
    - 공공용시설
    - 공장
    - 공동주택
    - 관광숙박시설
    - 관광휴게시설
    - 교정및군사시설
    - 교정시설
    - 교육연구및복지시설
    - 교육연구시설
    - 교회
    - 국방
    - 군사시설
    - 근린생활시설
    - 기숙사
    - 기타공공시설
    - 기타공장
    - 기타동식물관련시설
    - 기타사무소
    - 기타운동시설
    - 기타일반숙박시설
    - 기타자원순환관련시설
    - 기타제1종근린생활시설
    - 기타제2종근린생활시설
    - 기타창고시설
    - 기도원
    - 다가구주택
    - 다세대주택
    - 다중주택
    - 단란주점
    - 단독주택
    - 당구장
    - 도축장
    - 동물및식물관련시설
    - 동사무소
    - 마을공동시설
    - 묘지관련시설
    - 문화및집회시설
    - 발전시설
    - 방송통신시설
    - 변전소
    - 부대시설
    - 분뇨.쓰레기처리시설
    - 분뇨처리시설
    - 사당
    - 사무소
    - 사찰
    - 사회복지시설
    - 사진관
    - 상점(소매점)
    - 성당
    - 소매시장
    - 소매점
    - 소방서
    - 수련시설
    - 수리점
    - 숙박시설
    - 시장
    - 아동관련시설
    - 아파트
    - 야영장시설
    - 양수장
    - 어린이집
    - 업무시설
    - 여관
    - 연구소
    - 연립주택
    - 온실
    - 우체국
    - 운수시설
    - 운동시설
    - 위락시설
    - 위험물저장및처리시설
    - 유치원
    - 유흥주점
    - 의료시설
    - 일반공장
    - 일반업무시설
    - 일반음식점
    - 일반숙박시설
    - 자원순환관련시설
    - 자동차관련시설
    - 장례시설
    - 제실
    - 정비공장
    - 정수장
    - 제1종근린생활시설
    - 제2종근린생활시설
    - 제조업소
    - 종교시설
    - 종교집회장
    - 주유소
    - 창고시설
    - 축사
    - 체육도장
    - 파출소
    - 판매및영업시설
    - 판매시설
    - 학원
    - 휴게소
    - 휴게음식점
    - 휴양콘도미니엄
    - 화장실