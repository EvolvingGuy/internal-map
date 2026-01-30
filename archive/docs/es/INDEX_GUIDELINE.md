# ES 인덱스 추가 가이드라인

새 인덱스 추가 시 반드시 아래 순서대로 진행.

---

## 1. Document 생성

```kotlin
// es/document/land/{Name}Document.kt
data class {Name}Document(
    val pnu: String,
    val sd: String,
    val sgg: String,
    val emd: String,
    val land: Land,
    // ...
) {
    companion object {
        const val INDEX_NAME = "{index_name}"  // 단일 인덱스
        // 또는
        const val INDEX_PREFIX = "{prefix}"    // 파티션 인덱스
        const val PARTITION_COUNT = 4
        fun indexName(partition: Int) = "${INDEX_PREFIX}_$partition"
        fun allIndexNames() = (1..PARTITION_COUNT).map { indexName(it) }
    }
}
```

---

## 2. IndexingService 생성

```kotlin
// es/service/{name}/{Name}IndexingService.kt
```

**필수 메서드:**
- `reindex()` - 비동기 실행
- `count()` - 문서 수 조회
- `forcemerge()` - 비동기 실행
- `deleteIndex()` - 인덱스 삭제

**필수 설정:**
- `numberOfShards("4")`
- `numberOfReplicas("0")`
- `nestedObjects.limit(100000L)` (nested 사용 시)

---

## 3. AggregationService 생성

```kotlin
// es/service/{name}/{Name}AggregationService.kt
```

**필수 사항:**
- `.profile(true)` 반드시 포함
- `aggregateBySd()`, `aggregateBySgg()`, `aggregateByEmd()`
- 필터 없으면 LSRC 사용 (고정 클러스터)

```kotlin
val response = esClient.search({ s ->
    s.index(INDEX_NAME)
        .size(0)
        .profile(true)  // 필수!
        .query { ... }
        .aggregations("by_region") { ... }
}, Void::class.java)
```

---

## 4. REST Controller 생성

```kotlin
// controller/rest/{Name}RestController.kt      - reindex, forcemerge, count, delete
// controller/rest/{Name}AggRestController.kt   - agg/sd, agg/sgg, agg/emd
```

**엔드포인트 패턴:**
```
PUT    /api/es/{name}/reindex
PUT    /api/es/{name}/forcemerge
GET    /api/es/{name}/count
DELETE /api/es/{name}
GET    /api/es/{name}/agg/sd
GET    /api/es/{name}/agg/sgg
GET    /api/es/{name}/agg/emd
```

---

## 5. Web Controller 생성

```kotlin
// controller/web/{Name}AggWebController.kt
```

**필수 model 속성:**
- `naverMapClientId`
- `title`
- `apiPath`
- `level`
- `defaultZoom`

---

## 6. 템플릿 생성

```
resources/templates/es/{name}/agg.ftl
```

기존 템플릿 복사:
```bash
mkdir -p src/main/resources/templates/es/{name}
cp src/main/resources/templates/es/lnbtp/agg.ftl src/main/resources/templates/es/{name}/agg.ftl
```

---

## 7. Docker Compose 생성

```yaml
# docker-compose.{name}.yml
services:
  opensearch:
    image: opensearchproject/opensearch:2.11.1
    container_name: opensearch-{name}
    environment:
      - discovery.type=single-node
      - DISABLE_SECURITY_PLUGIN=true
      - OPENSEARCH_JAVA_OPTS=-Xms6g -Xmx6g
    ports:
      - "9200:9200"
    volumes:
      - geo_poc_os_{name}:/usr/share/opensearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 10s
      timeout: 5s
      retries: 10

  opensearch-dashboards:
    image: opensearchproject/opensearch-dashboards:2.11.1
    container_name: opensearch-dashboards-{name}
    environment:
      - OPENSEARCH_HOSTS=http://opensearch:9200
      - DISABLE_SECURITY_DASHBOARDS_PLUGIN=true
    ports:
      - "5601:5601"
    depends_on:
      opensearch:
        condition: service_healthy

volumes:
  geo_poc_os_{name}:
```

---

## 8. 빌드 & 실행

```bash
# 빌드 확인
./gradlew compileKotlin

# Docker 실행
docker-compose -f docker-compose.{name}.yml up -d

# 앱 실행
./gradlew clean bootRun

# 인덱싱 (비동기)
curl -X PUT http://localhost:3000/api/es/{name}/reindex

# 진행 확인
curl http://localhost:3000/api/es/{name}/count

# 조회 페이지
open http://localhost:3000/page/es/{name}/agg/sd
```

---

## 체크리스트

| Step | File | Check |
|------|------|-------|
| 1 | Document | INDEX_NAME 또는 INDEX_PREFIX 정의
| 2 | IndexingService | 비동기 reindex/forcemerge
| 3 | AggregationService | `.profile(true)` 포함
| 4 | RestController | 엔드포인트 매핑
| 5 | WebController | model 속성 설정
| 6 | Template | 템플릿 복사
| 7 | Docker | 볼륨명 고유하게
| 8 | Build | 컴파일 확인

---

## 네이밍 규칙

| Type | Pattern | Example |
|------|---------|---------|
| 단일 인덱스 | `land_{feature}` | `land_nested_building_trade_point`
| 파티션 인덱스 | `{abbr}_{n}` | `lnbtp_1`, `lnbtpu_1`
| Docker 볼륨 | `geo_poc_os_{abbr}` | `geo_poc_os_lnbtpp`
| 컨테이너 | `opensearch-{abbr}` | `opensearch-lnbtpp`
