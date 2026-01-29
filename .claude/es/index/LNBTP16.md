# LNBTP16 (Land Nested Building Trade Point 16-Shard)
필지 단위 단일 인덱스 - 16샤드 고병렬 버전

## 특징
- 인덱스 속도 느림 (다른 케이스들에 비해서. 기록 많이 살 길이네 동일 조건으로 계쏙 돌려보는 방법 뿐인듯)


## 인덱스 정보
| Key        | Value                                |
|------------|--------------------------------------|
| IndexName  | `land_nested_building_trade_point_16s` |
| Shards     | 16                                   |
| Replicas   | 0                                    |
| Type       | Single Index                         | 단일 인덱스

## 다른 인덱스와 비교
| Feature    | LNBTP   | LNBTPP    | LNBTPU    | LNBTPS   | LNBTP16  |
|------------|---------|-----------|-----------|----------|----------|
| Indices    | 1       | 4         | 4         | 17       | 1        | 인덱스 수
| Shards     | 3       | 4x4=16    | 4x4=16    | 17x4=68  | 16       | 총 샤드
| Partition  | None    | EMD hash  | PNU hash  | SD code  | None     | 파티션 방식
| Complexity | Low     | Medium    | Medium    | High     | Low      | 복잡도

## 특징
- **단일 인덱스**: 관리 단순
- **16샤드**: 병렬 처리 극대화
- **로컬 최적화**: 단일 노드에서 16코어 활용


## API 엔드포인트

### 관리 API
| Method | Endpoint                     |
|--------|------------------------------|
| PUT    | `/api/es/lnbtp16/reindex`    | 전체 재인덱싱 (비동기)
| PUT    | `/api/es/lnbtp16/forcemerge` | 세그먼트 병합 (비동기)
| GET    | `/api/es/lnbtp16/count`      | 인덱스 문서 수
| DELETE | `/api/es/lnbtp16`            | 인덱스 삭제

### 집계 API
| Method | Endpoint                   |
|--------|----------------------------|
| GET    | `/api/es/lnbtp16/agg/sd`   | 시도별 집계
| GET    | `/api/es/lnbtp16/agg/sgg`  | 시군구별 집계
| GET    | `/api/es/lnbtp16/agg/emd`  | 읍면동별 집계

### 웹 페이지
| Method | Endpoint                    |
|--------|-----------------------------|
| GET    | `/page/es/lnbtp16/agg/sd`   | 시도별 집계 지도
| GET    | `/page/es/lnbtp16/agg/sgg`  | 시군구별 집계 지도
| GET    | `/page/es/lnbtp16/agg/emd`  | 읍면동별 집계 지도

## 인덱싱 방식

### 비동기 처리
```kotlin
CoroutineScope(indexingDispatcher).launch {
    // 인덱싱 로직
}
```

### 처리 흐름
```
[1] ensureIndexExists()
    └─ 단일 인덱스 생성 (16샤드)
           ↓
[2] EMD 분배
    findDistinctEmdCodes() → 10개 워커에 라운드로빈 분배
           ↓
[3] 병렬 처리 (10 Worker)
    각 워커가 담당 EMD 목록을 순회
    ├─ EMD별 Stream 조회
    ├─ building summaries/outline 조회 (N건)
    ├─ trade 조회 (N건)
    ├─ Lnbtp16Document 생성
    └─ 단일 인덱스에 bulk indexing
           ↓
[4] Forcemerge
           ↓
[5] 결과 반환
```

### 인덱싱 설정
| Config     | Value |
|------------|-------|
| Worker     | 10    | EMD 라운드로빈 분배
| Fetch Size | 1,000 | JPA Stream 힌트
| Bulk Size  | 1,000 | ES bulk 단위
| Dispatcher | IO    | 코루틴 디스패처

### 로그 형식
```
[LNBTP16] Worker-0 벌크 #1/87,748: 1,000/87,747,632 (0.0%) EMD=11110101 (1/377), 건물 1,234건, 실거래 567건, 스텝 1,234ms, 누적 2.34s
```

## 집계 쿼리

### 필수 설정
```kotlin
val response = esClient.search({ s ->
    s.index("land_nested_building_trade_point_16s")
        .size(0)
        .profile(true)   // 필수!
        .query { ... }
        .aggregations("by_region") { ... }
}, Void::class.java)
```

### 필터 없는 경우
LSRC (고정형 클러스터)에서 미리 집계된 결과 반환 → 초고속

### 필터 있는 경우
16샤드 병렬 쿼리 후 집계

## 관련 파일
| Type            | Path                                              |
|-----------------|---------------------------------------------------|
| Document        | `es/document/land/Lnbtp16Document.kt`             |
| Indexing        | `es/service/lnbtp16/Lnbtp16IndexingService.kt`    |
| Aggregation     | `es/service/lnbtp16/Lnbtp16AggregationService.kt` |
| REST Controller | `controller/rest/Lnbtp16RestController.kt`        |
| REST Agg        | `controller/rest/Lnbtp16AggRestController.kt`     |
| Web Controller  | `controller/web/Lnbtp16AggWebController.kt`       |
| Template        | `templates/es/lnbtp16/agg.ftl`                    |
| Docker          | `docker-compose.lnbtp16.yml`                      | volume: `geo_poc_os_lnbtp16`


2026-01-29T19:45:24.401+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] field=sd, shards=16
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_1][1] - ConstantScoreQuery - 77.69ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_1][1] - TermQuery - 9.28ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_1][1] - agg:GlobalOrdinalsStringTermsAggregator - 45.51ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_2][1] - ConstantScoreQuery - 71.31ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_2][1] - TermQuery - 8.23ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_2][1] - agg:GlobalOrdinalsStringTermsAggregator - 45.13ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][0] - ConstantScoreQuery - 97.93ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][0] - TermQuery - 8.80ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][0] - agg:GlobalOrdinalsStringTermsAggregator - 47.82ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][3] - ConstantScoreQuery - 79.11ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][3] - TermQuery - 10.60ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_3][3] - agg:GlobalOrdinalsStringTermsAggregator - 47.57ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_4][2] - ConstantScoreQuery - 64.05ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_4][2] - TermQuery - 8.43ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [WRfRJLImT2WHA2tEA4VGsA][lnbtpu_4][2] - agg:GlobalOrdinalsStringTermsAggregator - 46.65ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][0] - ConstantScoreQuery - 101.48ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][0] - TermQuery - 11.56ms
2026-01-29T19:45:24.402+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][0] - agg:GlobalOrdinalsStringTermsAggregator - 83.40ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][3] - ConstantScoreQuery - 85.02ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][3] - TermQuery - 12.05ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_1][3] - agg:GlobalOrdinalsStringTermsAggregator - 71.80ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][0] - ConstantScoreQuery - 92.30ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][0] - TermQuery - 14.07ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][0] - agg:GlobalOrdinalsStringTermsAggregator - 107.78ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][3] - ConstantScoreQuery - 85.54ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][3] - TermQuery - 12.88ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_2][3] - agg:GlobalOrdinalsStringTermsAggregator - 72.51ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_3][2] - ConstantScoreQuery - 215.45ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_3][2] - TermQuery - 11.96ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_3][2] - agg:GlobalOrdinalsStringTermsAggregator - 73.65ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_4][1] - ConstantScoreQuery - 92.74ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_4][1] - TermQuery - 12.56ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [XdS2cbAoQpCO97Sfo3UO8g][lnbtpu_4][1] - agg:GlobalOrdinalsStringTermsAggregator - 74.85ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_1][2] - ConstantScoreQuery - 104.52ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_1][2] - TermQuery - 12.10ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_1][2] - agg:GlobalOrdinalsStringTermsAggregator - 74.60ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_2][2] - ConstantScoreQuery - 289.63ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_2][2] - TermQuery - 12.29ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_2][2] - agg:GlobalOrdinalsStringTermsAggregator - 71.94ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_3][1] - ConstantScoreQuery - 86.41ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_3][1] - TermQuery - 12.83ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_3][1] - agg:GlobalOrdinalsStringTermsAggregator - 76.30ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][0] - ConstantScoreQuery - 124.77ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][0] - TermQuery - 12.60ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][0] - agg:GlobalOrdinalsStringTermsAggregator - 2569.98ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][3] - ConstantScoreQuery - 113.04ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][3] - TermQuery - 12.65ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [iUjId3lOQpi5UxFXlgzXsw][lnbtpu_4][3] - agg:GlobalOrdinalsStringTermsAggregator - 75.70ms
2026-01-29T19:45:24.403+09:00  INFO 95618 --- [geo_poc] [nio-3000-exec-9] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Agg] field=sd, regions=17, totalCount=3447094, hasFilter=true, index=lnbtpu_*, ES took=425ms, total=480ms

2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] field=sd, shards=16
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][0] - ConstantScoreQuery - 45.82ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][0] - TermQuery - 5.95ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][0] - agg:GlobalOrdinalsStringTermsAggregator - 58.42ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][1] - ConstantScoreQuery - 48.65ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][1] - TermQuery - 4.37ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][1] - agg:GlobalOrdinalsStringTermsAggregator - 26.95ms
2026-01-29T19:46:59.552+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][2] - ConstantScoreQuery - 56.23ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][2] - TermQuery - 5.99ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][2] - agg:GlobalOrdinalsStringTermsAggregator - 40.55ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][3] - ConstantScoreQuery - 52.52ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][3] - TermQuery - 4.80ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_1][3] - agg:GlobalOrdinalsStringTermsAggregator - 29.74ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][0] - ConstantScoreQuery - 49.09ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][0] - TermQuery - 5.53ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][0] - agg:GlobalOrdinalsStringTermsAggregator - 45.49ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][1] - ConstantScoreQuery - 56.36ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][1] - TermQuery - 5.36ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][1] - agg:GlobalOrdinalsStringTermsAggregator - 52.98ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][2] - ConstantScoreQuery - 41.07ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][2] - TermQuery - 5.58ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][2] - agg:GlobalOrdinalsStringTermsAggregator - 45.91ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][3] - ConstantScoreQuery - 36.35ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][3] - TermQuery - 4.79ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_2][3] - agg:GlobalOrdinalsStringTermsAggregator - 30.21ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][0] - ConstantScoreQuery - 45.54ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][0] - TermQuery - 5.54ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][0] - agg:GlobalOrdinalsStringTermsAggregator - 31.24ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][1] - ConstantScoreQuery - 41.61ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][1] - TermQuery - 5.82ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][1] - agg:GlobalOrdinalsStringTermsAggregator - 52.13ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][2] - ConstantScoreQuery - 44.30ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][2] - TermQuery - 8.39ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][2] - agg:GlobalOrdinalsStringTermsAggregator - 27.95ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][3] - ConstantScoreQuery - 39.32ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][3] - TermQuery - 4.37ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_3][3] - agg:GlobalOrdinalsStringTermsAggregator - 32.01ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][0] - ConstantScoreQuery - 50.51ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][0] - TermQuery - 5.10ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][0] - agg:GlobalOrdinalsStringTermsAggregator - 132.71ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][1] - ConstantScoreQuery - 47.59ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][1] - TermQuery - 5.50ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][1] - agg:GlobalOrdinalsStringTermsAggregator - 52.95ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][2] - ConstantScoreQuery - 46.38ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][2] - TermQuery - 4.77ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][2] - agg:GlobalOrdinalsStringTermsAggregator - 82.16ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][3] - ConstantScoreQuery - 38.83ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][3] - TermQuery - 4.90ms
2026-01-29T19:46:59.553+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Profile] [hVNKPm0uRHmwmHrd4rWGbg][lnbtpu_4][3] - agg:GlobalOrdinalsStringTermsAggregator - 111.03ms
2026-01-29T19:46:59.554+09:00  INFO 950 --- [geo_poc] [io-3000-exec-10] c.d.g.e.s.l.LnbtpuAggregationService     : [LNBTPU Agg] field=sd, regions=17, totalCount=3447094, hasFilter=true, index=lnbtpu_*, ES took=311ms, total=334ms