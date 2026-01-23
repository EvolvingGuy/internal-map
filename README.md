# geo_poc

지리기반 부동산 필지 데이터 PoC 프로젝트

## Tech Stack

| Category      | Technology           | Version |
|---------------|----------------------|---------|
| Language      | Kotlin               | 2.0.21  |
| Framework     | Spring Boot          | 3.5.9   |
| JDK           | Java                 | 21      |
| Database      | PostgreSQL + PostGIS | -       |
| Search Engine | Elasticsearch        | 8.x     |
| Geo Library   | H3 (Uber)            | 4.1.1   |
| Geo Library   | JTS                  | 1.20.0  |
| Template      | Freemarker           | -       |
| Build         | Gradle               | 8.x     |

## Dependencies

```kotlin
// Spring Boot Starters
spring-boot-starter-data-jpa
spring-boot-starter-data-elasticsearch
spring-boot-starter-web
spring-boot-starter-freemarker
spring-boot-starter-actuator

// Database
postgresql (runtime)
hibernate-spatial

// Geo
com.uber:h3:4.1.1
org.locationtech.jts:jts-core:1.20.0

// Kotlin
kotlin-reflect
kotlinx-coroutines-core:1.10.1
jackson-module-kotlin
jackson-datatype-jsr310
```

## Configuration

```yaml
server.port: 3000
spring.datasource.url: jdbc:postgresql://localhost:6432/rtb_int
spring.elasticsearch.uris: http://localhost:9200
```

---

## REST API

Base URL: `http://localhost:3000`

### LSRC (고정형 행정구역 클러스터)

| Method | Endpoint                                             |
|--------|------------------------------------------------------|
| PUT    | `/api/es/lsrc/reindex`                               | 전체 재인덱싱
| PUT    | `/api/es/lsrc/forcemerge`                            | Forcemerge 실행
| GET    | `/api/es/lsrc/count`                                 | 인덱스 문서 수
| DELETE | `/api/es/lsrc`                                       | 인덱스 삭제
| GET    | `/api/es/lsrc/query/sd?swLng=&swLat=&neLng=&neLat=`  | 시도 조회
| GET    | `/api/es/lsrc/query/sgg?swLng=&swLat=&neLng=&neLat=` | 시군구 조회
| GET    | `/api/es/lsrc/query/emd?swLng=&swLat=&neLng=&neLat=` | 읍면동 조회

### LDRC (동적 행정구역 클러스터)

| Method | Endpoint                                                         |
|--------|------------------------------------------------------------------|
| PUT    | `/api/es/ldrc/reindex`                                           | 전체 재인덱싱 (EMD→SGG→SD) 
| PUT    | `/api/es/ldrc/reindex/emd`                                       | EMD만 인덱싱 
| PUT    | `/api/es/ldrc/reindex/sgg`                                       | SGG만 인덱싱 
| PUT    | `/api/es/ldrc/reindex/sd`                                        | SD만 인덱싱 
| PUT    | `/api/es/ldrc/forcemerge`                                        | Forcemerge 실행 
| GET    | `/api/es/ldrc/count`                                             | 인덱스 문서 수 
| GET    | `/api/es/ldrc/clusters?swLng=&swLat=&neLng=&neLat=&level=&zoom=` | 클러스터 조회 

### LC (필지 단위 필터링)

| Method | Endpoint                |
|--------|-------------------------|
| PUT    | `/api/es/lc/reindex`    | 전체 재인덱싱 (백그라운드) 
| PUT    | `/api/es/lc/forcemerge` | Forcemerge 실행 
| GET    | `/api/es/lc/count`      | 인덱스 문서 수 
| DELETE | `/api/es/lc`            | 인덱스 삭제 

### LC Aggregation (필터 + 집계)

| Method | Endpoint                                                                                   |
|--------|--------------------------------------------------------------------------------------------|
| GET    | `/api/es/lc/agg/sd?swLng=&swLat=&neLng=&neLat=&{filters}`                                  | 시도별 집계
| GET    | `/api/es/lc/agg/sgg?swLng=&swLat=&neLng=&neLat=&{filters}`                                 | 시군구별 집계
| GET    | `/api/es/lc/agg/emd?swLng=&swLat=&neLng=&neLat=&{filters}`                                 | 읍면동별 집계
| GET    | `/api/es/lc/agg/grid?swLng=&swLat=&neLng=&neLat=&viewportWidth=&viewportHeight=&{filters}` | 그리드 집계

**Filter Parameters:**
- Building: `buildingMainPurpsCdNm`, `buildingRegstrGbCdNm`, `buildingUseAprDayStart/End`, `buildingTotAreaMin/Max`, ...
- Land: `landJiyukCd1`, `landJimokCd`, `landAreaMin/Max`, `landPriceMin/Max`
- Trade: `tradeProperty`, `tradeContractDateStart/End`, `tradeEffectiveAmountMin/Max`, ...

### Registration (등기)

| Method | Endpoint                          |
|--------|-----------------------------------|
| PUT    | `/api/es/registration/reindex`    | 전체 재인덱싱
| POST   | `/api/es/registration/forcemerge` | Forcemerge 실행
| GET    | `/api/es/registration/count`      | 인덱스 문서 수
| DELETE | `/api/es/registration`            | 인덱스 삭제

---

## Web Pages

Base URL: `http://localhost:3000`

### LSRC Pages (고정형 클러스터 지도)

| URL                                      |
|------------------------------------------|
| `http://localhost:3000/page/es/lsrc/sd`  | 시도 클러스터
| `http://localhost:3000/page/es/lsrc/sgg` | 시군구 클러스터
| `http://localhost:3000/page/es/lsrc/emd` | 읍면동 클러스터

### LDRC Pages (동적 클러스터 지도)

| URL                                      |
|------------------------------------------|
| `http://localhost:3000/page/es/ldrc/sd`  | 시도 동적 클러스터
| `http://localhost:3000/page/es/ldrc/sgg` | 시군구 동적 클러스터
| `http://localhost:3000/page/es/ldrc/emd` | 읍면동 동적 클러스터

### LC Pages (필터 + 집계 지도)

| URL                                         |
|---------------------------------------------|
| `http://localhost:3000/page/es/lc/agg/sd`   | 시도 필터 집계 
| `http://localhost:3000/page/es/lc/agg/sgg`  | 시군구 필터 집계 
| `http://localhost:3000/page/es/lc/agg/emd`  | 읍면동 필터 집계 
| `http://localhost:3000/page/es/lc/agg/grid` | 그리드 필터 집계 

---

## Documentation

자세한 내용은 `.claude/` 폴더 참조:

```
.claude/
├── CLAUDE.md          # AI 개발 지침
├── BUSINESS.md        # 비즈니스 정보 (행정구역, 줌 매핑)
├── DOCUMENT.md        # 문서화 가이드
├── ES_OVERVIEW.md     # ES 전체 개요
├── REQUIREMENT.md     # 요구사항
├── es/
│   ├── ES_CONCEPT.md  # ES 개념 (노드, 샤드, 레플리카)
│   ├── ES_GUIDELINE.md # ES 운영 지침
│   └── index/
│       ├── LSRC.md    # LSRC 인덱스 스펙
│       ├── LDRC.md    # LDRC 인덱스 스펙
│       └── LC.md      # LC 인덱스 스펙
└── sql/
    ├── land_characteristic.md
    ├── boundary_region.md
    ├── building_ledger_outline.md
    ├── r3_pnu_agg_emd_10.md
    ├── r3_real_estate_trade.md
    └── r3_registration.md
```
