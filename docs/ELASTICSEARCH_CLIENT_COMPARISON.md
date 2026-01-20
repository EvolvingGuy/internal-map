# Elasticsearch Client vs Spring Data Elasticsearch 비교 분석

> Spring Boot 4.0.1 + Spring Data Elasticsearch 6.0.1 + Elasticsearch 9.x 기준

## 현재 환경

| 구성 요소 | 버전 |
|-----------|------|
| Spring Boot | 4.0.1 |
| Spring Framework | 7.0.x |
| Spring Data Elasticsearch | 6.0.1 |
| Elasticsearch Java Client | 9.2.2 |
| Elasticsearch Server (권장) | 9.x |

---

## 1. Elasticsearch Java Client (직접 사용)

### 개요
```kotlin
// 의존성
implementation("co.elastic.clients:elasticsearch-java:9.2.2")
```

Elastic에서 제공하는 공식 Java Client로, ES API를 직접 호출합니다.

### 장점

| 항목 | 설명 |
|------|------|
| **최신 기능 즉시 지원** | ES 신규 기능이 나오면 바로 사용 가능 |
| **API 완전 제어** | 쿼리, 집계, 스크립트 등 모든 ES 기능 직접 사용 |
| **타입 안전성** | Fluent Builder 패턴으로 컴파일 타임 검증 |
| **경량** | Spring Data 레이어 없이 바로 ES 통신 |
| **복잡한 쿼리** | Geo Query, Aggregation 등 복잡한 쿼리에 유리 |

### 단점

| 항목 | 설명 |
|------|------|
| **보일러플레이트** | 엔티티 매핑, 인덱스 관리 직접 구현 필요 |
| **Spring 통합 부재** | 트랜잭션, Repository 패턴 미지원 |
| **학습 곡선** | ES DSL 및 Client API 학습 필요 |
| **일관성 부족** | 다른 Spring Data 모듈과 패턴 불일치 |

### 사용 예시
```kotlin
@Service
class PnuEsService(
    private val esClient: ElasticsearchClient
) {
    fun searchByH3Index(h3Index: Long): SearchResponse<PnuDocument> {
        return esClient.search({ s ->
            s.index("pnu_agg")
                .query { q ->
                    q.term { t ->
                        t.field("h3_index").value(h3Index)
                    }
                }
        }, PnuDocument::class.java)
    }

    fun searchByGeoBox(bbox: BBox): SearchResponse<PnuDocument> {
        return esClient.search({ s ->
            s.index("pnu_agg")
                .query { q ->
                    q.geoBoundingBox { g ->
                        g.field("location")
                            .boundingBox { b ->
                                b.tlbr { t ->
                                    t.topLeft { tl -> tl.latlon { ll -> ll.lat(bbox.neLat).lon(bbox.swLng) } }
                                     .bottomRight { br -> br.latlon { ll -> ll.lat(bbox.swLat).lon(bbox.neLng) } }
                                }
                            }
                    }
                }
        }, PnuDocument::class.java)
    }
}
```

---

## 2. Spring Data Elasticsearch (Repository 패턴)

### 개요
```kotlin
// 의존성 (Spring Boot Starter가 관리)
implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
```

Spring Data의 Repository 추상화를 통해 ES를 사용합니다.

### 장점

| 항목 | 설명 |
|------|------|
| **일관된 패턴** | JPA, Redis 등 다른 Spring Data와 동일한 Repository 패턴 |
| **자동 매핑** | `@Document`, `@Field` 어노테이션으로 자동 매핑 |
| **인덱스 관리** | 인덱스 생성/매핑 자동화 지원 |
| **메서드 네이밍 쿼리** | `findByH3Index(h3Index)` 같은 직관적인 쿼리 |
| **Spring 생태계 통합** | AOP, 트랜잭션 관리, 테스트 지원 |
| **학습 곡선 낮음** | Spring Data 경험자는 빠른 적응 |

### 단점

| 항목 | 설명 |
|------|------|
| **추상화 오버헤드** | 단순 쿼리에도 Repository 레이어 통과 |
| **버전 지연** | ES 신기능이 Spring Data에 반영되기까지 지연 |
| **복잡한 쿼리 한계** | 복잡한 Aggregation, Script 쿼리는 결국 Native Query 필요 |
| **Geo Query 지원 제한** | 일부 Geo 기능은 Native 쿼리로 처리해야 함 |

### 사용 예시
```kotlin
@Document(indexName = "pnu_agg")
data class PnuDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Long)
    val h3Index: Long,

    @Field(type = FieldType.Long)
    val code: Long,

    @Field(type = FieldType.Integer)
    val cnt: Int,

    @GeoPointField
    val location: GeoPoint
)

interface PnuEsRepository : ElasticsearchRepository<PnuDocument, String> {
    fun findByH3Index(h3Index: Long): List<PnuDocument>
    fun findByH3IndexIn(h3Indexes: Collection<Long>): List<PnuDocument>
    fun findByCodeAndH3Index(code: Long, h3Index: Long): PnuDocument?
}

@Service
class PnuEsService(
    private val repository: PnuEsRepository,
    private val operations: ElasticsearchOperations  // 복잡한 쿼리용
) {
    fun searchByH3Indexes(h3Indexes: List<Long>): List<PnuDocument> {
        return repository.findByH3IndexIn(h3Indexes)
    }

    // 복잡한 쿼리는 Operations 사용
    fun searchByGeoBox(bbox: BBox): List<PnuDocument> {
        val query = NativeQuery.builder()
            .withQuery { q ->
                q.geoBoundingBox { g ->
                    g.field("location")
                        .boundingBox { /* ... */ }
                }
            }
            .build()
        return operations.search(query, PnuDocument::class.java)
            .map { it.content }
            .toList()
    }
}
```

---

## 3. 하이브리드 접근법 (권장)

### 개요
Spring Data Elasticsearch를 기본으로 사용하되, 복잡한 쿼리에는 `ElasticsearchOperations` 또는 직접 `ElasticsearchClient`를 사용합니다.

### 구조
```
┌─────────────────────────────────────────────────────────────┐
│                        Service Layer                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ EsRepository    │  │ ElasticsearchOps│  │ EsClient    │  │
│  │ (단순 CRUD)      │  │ (중간 복잡도)    │  │ (고급 쿼리)  │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
│           │                   │                   │          │
│           └───────────────────┼───────────────────┘          │
│                               ▼                              │
│                    Elasticsearch Client                      │
│                    (elasticsearch-java 9.2.2)                │
└─────────────────────────────────────────────────────────────┘
```

### 사용 케이스 분류

| 케이스 | 권장 방식 | 예시 |
|--------|----------|------|
| 단순 CRUD | Repository | `findById`, `save`, `deleteById` |
| 단순 검색 | Repository | `findByH3Index`, `findByCodeIn` |
| Multi-field 검색 | Repository/Operations | `findByH3IndexAndCode` |
| Geo Bounding Box | Operations/Client | 뷰포트 기반 검색 |
| Aggregation | Client | H3별 카운트 집계 |
| Script Query | Client | 동적 스코어링 |
| Bulk 작업 | Client | 대량 인덱싱 |

---

## 4. 이 프로젝트 맥락에서의 판단

### 현재 요구사항 분석

| 기능 | 복잡도 | 권장 방식 |
|------|--------|----------|
| H3 인덱스 기반 조회 | 낮음 | Repository |
| BBox(뷰포트) 기반 Geo 검색 | 중간 | Operations |
| 행정구역 코드별 그루핑 | 중간 | Operations |
| H3별 필지 카운트 집계 | 중간~높음 | Client/Operations |
| 바둑판 그리드 클러스터링 | 높음 | Client |

### 권장 사항: **하이브리드 접근법**

1. **기본 구조**: Spring Data Elasticsearch Repository 사용
   - 기존 JPA Repository 패턴과 일관성 유지
   - 팀 학습 비용 최소화

2. **Geo/Aggregation 쿼리**: `ElasticsearchOperations` 활용
   - NativeQuery로 ES DSL 직접 작성
   - Spring 추상화 레이어 유지

3. **고급 최적화**: 필요 시 `ElasticsearchClient` 직접 사용
   - Bulk 인덱싱
   - 복잡한 Script 쿼리

---

## 5. 설정 예시

### application.properties
```properties
# Elasticsearch 연결 설정
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=changeme

# 인덱스 자동 생성 (개발 환경)
spring.data.elasticsearch.repositories.enabled=true
```

### Configuration (하이브리드)
```kotlin
@Configuration
class ElasticsearchConfig {

    // Spring Data가 자동 설정하는 ElasticsearchClient 재사용
    // 추가 설정이 필요한 경우에만 커스터마이징
}
```

---

## 6. 결론

| 기준 | ES Client 직접 사용 | Spring Data ES | 하이브리드 |
|------|---------------------|----------------|-----------|
| 개발 속도 | 느림 | 빠름 | 중간 |
| 유연성 | 높음 | 중간 | 높음 |
| 유지보수 | 중간 | 높음 | 높음 |
| 학습 비용 | 높음 | 낮음 | 중간 |
| **이 프로젝트 적합도** | 중간 | 중간 | **높음** |

### 최종 권장: **하이브리드 접근법**

- 기존 RDS 기반 코드와의 일관성 (Repository 패턴)
- Geo Query 및 Aggregation 필요 시 유연한 대응
- Spring Boot 4.0.1 자동 설정 활용

---

## 참고 자료

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Data Elasticsearch Reference](https://docs.spring.io/spring-data/elasticsearch/reference/index.html)
- [Elasticsearch Java Client Documentation](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/installation.html)
- [Spring Data Elasticsearch Versions](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html)
