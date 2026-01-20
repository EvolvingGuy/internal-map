# 지도 기반 필지 클러스터링 필터링 아키텍처 검토 문서

## 1. 배경 및 문제 상황

### 1.1 현재 시스템 개요
- **서비스 유형**: B2B 지도 기반 필지 정보 서비스
- **데이터 규모**: 약 3,700만 필지 (PNU 기준)
- **기술 스택**: Spring Boot, PostgreSQL, Redis, H3 (Uber의 육각형 그리드 시스템)

### 1.2 현재 구현된 기능
1. **Count**: 뷰포트 내 필지 총 개수
2. **Dynamic**: 가변형 행정구역별 필지 클러스터링
3. **Static**: 고정형 행정구역별 전체 필지 카운트
4. **Grid**: 바둑판식 그리드 클러스터링
5. **Indicator**: 화면 중심점 행정구역 표시

### 1.3 현재 아키텍처
```
[Client] → [API Server] → [Redis Cache] → [PostgreSQL]
                              ↓
                    사전 집계 테이블 (r3_pnu_agg_*)
```

**사전 집계 테이블 구조:**
```sql
CREATE TABLE manage.r3_pnu_agg_emd_10 (
    code     bigint NOT NULL,           -- 행정구역 코드
    h3_index bigint NOT NULL,           -- H3 인덱스
    cnt      int NOT NULL,              -- 필지 개수
    sum_lat  double precision NOT NULL, -- 위도 합산 (중심점 계산용)
    sum_lng  double precision NOT NULL, -- 경도 합산 (중심점 계산용)
    PRIMARY KEY (code, h3_index)
);
```

### 1.4 문제점
현재 사전 집계 방식은 **전체 필지 기준**으로만 집계되어 있음.

실제 서비스 요구사항:
- 지목(jimok_cd), 용도지역(jiyuk_cd_1), 건물용도(land_use_cd) 등 **다양한 필터 조건**으로 필터링된 결과를 클러스터링해서 보여줘야 함
- 필터 조건의 조합이 매우 다양함 (수십~수백 가지)
- 필터 조건별로 사전 집계 테이블을 만드는 것은 **조합 폭발**로 불가능

---

## 2. 해결 방안 비교: Elasticsearch vs Redis 캐시 기반

### 2.1 Option A: Elasticsearch 도입

**구조:**
```
[Client] → [API Server] → [Elasticsearch Cluster]
                              ↓
                    실시간 필터링 + 집계 (Aggregation)
```

**장점:**
- 복잡한 필터 조건 + 집계를 네이티브로 지원
- Geo Aggregation 기능 내장
- 필터 조합에 관계없이 일관된 성능

**단점:**
- 운영 복잡도 증가 (클러스터 관리, 샤딩, 레플리카)
- 인프라 비용 증가 (ES 클러스터 최소 3노드 권장)
- 데이터 동기화 파이프라인 필요
- 러닝커브
- B2B 저트래픽 서비스에 오버스펙

---

### 2.2 Option B: Redis 캐시 기반 실시간 집계 (제안)

**구조:**
```
[Client] → [API Server] → [Redis Cache (해시 기반 키)]
                              ↓ (캐시 미스)
                    [PostgreSQL 실시간 집계]
                              ↓
                    PNU-H3 인덱싱 테이블 + 필터 쿼리
```

**핵심 아이디어:**
1. **필터 조건을 해시**하여 캐시 키로 사용
2. **PNU-H3 매핑 테이블**로 공간 인덱싱
3. **실시간 필터링 집계** 후 캐시 저장
4. **동일 필터 조합**은 캐시에서 즉시 응답

---

## 3. 제안 아키텍처 상세

### 3.1 PNU-H3 인덱싱 테이블

**목적:**
- H3 인덱스로 해당 영역의 PNU 목록을 빠르게 조회
- 여러 Resolution으로 준비하여 줌 레벨별 최적화

**테이블 구조:**
```sql
-- Resolution 10 (줌 레벨 16~22, 상세)
CREATE TABLE manage.r3_pnu_h3_10 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);

-- Resolution 7 (줌 레벨 12~15, 중간)
CREATE TABLE manage.r3_pnu_h3_7 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);

-- Resolution 5 (줌 레벨 0~11, 광역)
CREATE TABLE manage.r3_pnu_h3_5 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);
```

**데이터 생성 쿼리 (예시 - Resolution 7):**
```sql
INSERT INTO manage.r3_pnu_h3_7 (pnu, h3_index)
SELECT pnu, h3_lat_lng_to_cell(center::point, 7)::bigint
FROM external_data.land_characteristic
WHERE pnu IS NOT NULL AND center IS NOT NULL;
```

**H3 Resolution별 특성:**

| Resolution | 셀 면적 | 한국 전체 셀 수 | 용도 |
|------------|---------|----------------|------|
| 5 | ~252 km² | ~400개 | 광역 조회 |
| 7 | ~5 km² | ~20,000개 | 중간 조회 |
| 10 | ~0.015 km² | ~6,000,000개 | 상세 조회 |

---

### 3.2 필터 파라미터 해시 전략

**FilterCondition 클래스:**
```kotlin
data class FilterCondition(
    val jimokCd: String? = null,      // 지목 코드
    val jiyukCd1: String? = null,     // 용도지역 코드 1
    val jiyukCd2: String? = null,     // 용도지역 코드 2
    val landUseCd: String? = null,    // 토지이용 코드
    val heightCd: String? = null,     // 높이 코드
    val shapeCd: String? = null,      // 형상 코드
    val roadCd: String? = null,       // 도로 코드
    val priceMin: Int? = null,        // 최소 공시지가
    val priceMax: Int? = null         // 최대 공시지가
) {
    /**
     * 필터 조건을 해시 문자열로 변환
     * - null 값은 해시에서 제외
     * - 정렬된 키 순서로 일관된 해시 생성
     */
    fun toHashKey(): String {
        if (isEmpty()) return "nofilter"

        val parts = mutableListOf<String>()
        jimokCd?.let { parts.add("j:$it") }
        jiyukCd1?.let { parts.add("y1:$it") }
        jiyukCd2?.let { parts.add("y2:$it") }
        landUseCd?.let { parts.add("lu:$it") }
        heightCd?.let { parts.add("h:$it") }
        shapeCd?.let { parts.add("s:$it") }
        roadCd?.let { parts.add("r:$it") }
        priceMin?.let { parts.add("pmin:$it") }
        priceMax?.let { parts.add("pmax:$it") }

        // MD5 또는 SHA256으로 짧은 해시 생성
        return parts.sorted().joinToString("|").md5()
    }

    fun isEmpty(): Boolean =
        jimokCd == null && jiyukCd1 == null && jiyukCd2 == null &&
        landUseCd == null && heightCd == null && shapeCd == null &&
        roadCd == null && priceMin == null && priceMax == null

    /**
     * SQL WHERE 절 생성
     */
    fun toWhereClause(): String {
        val conditions = mutableListOf<String>()
        jimokCd?.let { conditions.add("jimok_cd = '$it'") }
        jiyukCd1?.let { conditions.add("jiyuk_cd_1 = '$it'") }
        jiyukCd2?.let { conditions.add("jiyuk_cd_2 = '$it'") }
        landUseCd?.let { conditions.add("land_use_cd = '$it'") }
        heightCd?.let { conditions.add("height_cd = '$it'") }
        shapeCd?.let { conditions.add("shape_cd = '$it'") }
        roadCd?.let { conditions.add("road_cd = '$it'") }
        priceMin?.let { conditions.add("CAST(price AS INT) >= $it") }
        priceMax?.let { conditions.add("CAST(price AS INT) <= $it") }

        return if (conditions.isEmpty()) "1=1"
               else conditions.joinToString(" AND ")
    }
}
```

---

### 3.3 캐시 키 설계

**캐시 키 패턴:**
```
{filter_hash}:{service}:{resolution}:{h3_index}
```

**예시:**
```
# 무필터 - Count 서비스, Resolution 10, H3 인덱스 617700169518678015
nofilter:count:10:617700169518678015

# 지목=대 필터 - Dynamic 서비스, Resolution 7
a3f2b1c4:dynamic:7:608533827135545343

# 복합 필터 (지목=대, 용도지역=상업)
f7e8d9c0:grid:10:617700169518678015
```

**캐시 TTL 전략:**
```kotlin
object CacheTTL {
    // 무필터: 영구 캐시 (1일, 데이터 변경 시에만 갱신)
    val NO_FILTER = Duration.ofDays(1)

    // 필터 있음: LRU evict에 맡김 (6시간)
    val WITH_FILTER = Duration.ofHours(6)
}
```

---

### 3.4 실시간 집계 쿼리 예시

**Count 서비스 (필터 적용):**
```sql
-- 1단계: H3 인덱스로 PNU 목록 조회
-- 2단계: 필터 조건으로 카운트

SELECT COUNT(*) as cnt
FROM external_data.land_characteristic lc
WHERE lc.pnu IN (
    SELECT pnu FROM manage.r3_pnu_h3_10
    WHERE h3_index IN (617700169518678015, 617700169518678016, ...)
)
AND lc.jimok_cd = '대'
AND lc.jiyuk_cd_1 = '상업';
```

**Dynamic 서비스 (필터 적용, 행정구역별 그룹핑):**
```sql
SELECT
    LEFT(lc.bjdong_cd, 10)::bigint as code,
    COUNT(*) as cnt,
    SUM(ST_Y(lc.center)) as sum_lat,
    SUM(ST_X(lc.center)) as sum_lng
FROM external_data.land_characteristic lc
WHERE lc.pnu IN (
    SELECT pnu FROM manage.r3_pnu_h3_7
    WHERE h3_index IN (...)
)
AND lc.jimok_cd = '대'
GROUP BY LEFT(lc.bjdong_cd, 10);
```

---

### 3.5 필요한 인덱스

```sql
-- land_characteristic 테이블에 필터 컬럼 인덱스 추가
CREATE INDEX idx_land_jimok ON external_data.land_characteristic (jimok_cd);
CREATE INDEX idx_land_jiyuk1 ON external_data.land_characteristic (jiyuk_cd_1);
CREATE INDEX idx_land_landuse ON external_data.land_characteristic (land_use_cd);

-- 복합 인덱스 (자주 사용되는 필터 조합)
CREATE INDEX idx_land_filter_combo ON external_data.land_characteristic
    (jimok_cd, jiyuk_cd_1, land_use_cd);

-- PNU 인덱스 (이미 존재)
CREATE INDEX land_characteristic_pnu_index ON external_data.land_characteristic (pnu);
```

---

## 4. 서비스별 적용 방안

### 4.1 Count 서비스

**현재:**
```
bbox → H3 indexes → r3_pnu_agg_* → SUM(cnt)
```

**필터 적용:**
```
bbox → H3 indexes → 캐시 조회 (filter:{hash}:count:{res}:{h3})
    → 캐시 히트: 즉시 반환
    → 캐시 미스:
        → r3_pnu_h3_{res}에서 PNU 목록
        → land_characteristic WHERE 필터 + PNU IN → COUNT(*)
        → 캐시 저장 → 반환
```

**난이도:** ⭐ (가장 단순, PoC 적합)

---

### 4.2 Dynamic 서비스 (가변형 행정구역)

**현재:**
```
bbox → H3 indexes → r3_pnu_agg_* → GROUP BY code → 행정구역별 집계
```

**필터 적용:**
```
bbox → H3 indexes → 캐시 조회 (filter:{hash}:dynamic:{res}:{h3})
    → 캐시 미스:
        → r3_pnu_h3_{res}에서 PNU 목록
        → land_characteristic WHERE 필터 + PNU IN
          → GROUP BY bjdong_cd → (code, cnt, sum_lat, sum_lng)
        → 캐시 저장 → 반환
    → 응답에서 code 기준 그룹핑
```

**난이도:** ⭐⭐

---

### 4.3 Grid 서비스 (바둑판)

**현재:**
```
bbox → H3 indexes → r3_pnu_agg_* → 바둑판 그리드 분배
```

**필터 적용:**
```
bbox → H3 indexes → 캐시 조회
    → 캐시 미스:
        → r3_pnu_h3_{res}에서 PNU 목록
        → land_characteristic WHERE 필터 + PNU IN
          → 각 PNU의 center 좌표 조회
        → 바둑판 그리드에 분배
        → 캐시 저장 → 반환
```

**난이도:** ⭐⭐

---

### 4.4 Static 서비스 (고정형 행정구역)

**현재:**
```
bbox → H3 indexes → region codes → r3_pnu_agg_static_region (전체 카운트)
```

**필터 적용:**
```
bbox → region codes → 캐시 조회 (filter:{hash}:static:{level}:{regionCode})
    → 캐시 미스:
        → land_characteristic WHERE bjdong_cd LIKE '{regionCode}%' AND 필터
          → COUNT(*)
        → 캐시 저장 → 반환
```

**주의:** Static은 "행정구역 전체 카운트"이므로 필터 적용 시 의미가 달라질 수 있음. 비즈니스 요구사항 확인 필요.

**난이도:** ⭐⭐⭐

---

### 4.5 Indicator 서비스

**현재 그대로 유지** - 필터와 무관하게 위치 기반 행정구역 표시

---

## 5. 성능 예상

### 5.1 응답 시간 예상

| 상황 | 예상 응답 시간 |
|------|---------------|
| 캐시 히트 | **< 10ms** |
| 캐시 미스 (res 10, 상세 영역) | 100 ~ 500ms |
| 캐시 미스 (res 7, 중간 영역) | 50 ~ 200ms |
| 캐시 미스 (res 5, 광역 영역) | 200 ~ 1000ms |

### 5.2 캐시 히트율 예상

B2B 서비스 특성상:
- 사용자 수 제한적
- 비슷한 영역/필터 조합 반복 조회 가능성 높음
- 예상 캐시 히트율: **60~80%**

자주 사용되는 필터 조합 워밍업 시:
- 예상 캐시 히트율: **80~95%**

### 5.3 Redis 메모리 사용량 예상

**캐시 키당 평균 크기:**
- Count: ~100 bytes
- Dynamic: ~1KB (행정구역 목록)
- Grid: ~2KB (그리드 셀 목록)

**예상 시나리오 (필터 조합 100개, H3 셀 10,000개 캐시):**
```
100 * 10,000 * 1KB = 1GB
```

Redis t4g.medium (6.5GB) 기준 충분히 여유 있음.

---

## 6. 장단점 비교

### 6.1 Elasticsearch 방식

| 장점 | 단점 |
|------|------|
| 복잡한 집계 네이티브 지원 | 운영 복잡도 높음 |
| 일관된 성능 | 인프라 비용 높음 (클러스터 3노드+) |
| Geo Aggregation 내장 | 데이터 동기화 파이프라인 필요 |
| 스케일아웃 용이 | 러닝커브 |

### 6.2 Redis 캐시 기반 방식 (제안)

| 장점 | 단점 |
|------|------|
| 운영 복잡도 낮음 | 콜드 부트 느림 (첫 요청) |
| 인프라 비용 낮음 | 복잡한 집계 쿼리 직접 구현 |
| 기존 인프라 활용 | 캐시 미스 시 DB 부하 |
| 캐시 히트 시 ES보다 빠름 | 필터 조합 폭발 시 히트율 저하 |
| 스케일업 간단 (Redis 사양 증가) | |

### 6.3 B2B 서비스 관점

- **트래픽**: 매우 낮음 (동시 사용자 수십 명 수준)
- **허용 응답 시간**: 1초 내외 허용 가능
- **운영 인력**: 제한적
- **비용 민감도**: 높음

**결론: Redis 캐시 기반 방식이 B2B 서비스에 적합**

---

## 7. 리스크 및 완화 방안

### 7.1 콜드 부트 (캐시 미스)

**리스크:** 처음 요청 시 실시간 집계로 응답 느림 (500ms ~ 1s)

**완화:**
- 자주 사용되는 필터 조합 워밍업 스크립트
- 서비스 시작 시 주요 영역 + 주요 필터 캐시 프리로딩
- 배치로 주기적 캐시 갱신

### 7.2 캐시 키 폭발

**리스크:** 필터 조합 * H3 셀 * Resolution → 캐시 키 수 폭발

**완화:**
- Redis maxmemory-policy: allkeys-lru (LRU eviction)
- 무필터는 긴 TTL, 필터는 짧은 TTL
- H3 Resolution 낮춰서 셀 수 줄이기

### 7.3 DB 부하

**리스크:** 캐시 미스 시 PostgreSQL 실시간 집계 부하

**완화:**
- land_characteristic 테이블 필터 컬럼 인덱스
- PNU IN 절 최적화 (서브쿼리 vs JOIN 비교)
- 커넥션 풀 관리
- 읽기 전용 레플리카 활용 가능

---

## 8. 구현 순서

### Phase 1: 인프라 준비
1. r3_pnu_h3_7, r3_pnu_h3_5 테이블 생성 (INSERT)
2. land_characteristic 필터 컬럼 인덱스 추가
3. 쿼리 성능 테스트

### Phase 2: 기본 구조 구현
4. FilterCondition 클래스 + 해시 로직
5. 캐시 서비스 확장 (해시 기반 키 지원)
6. 캐시 TTL 전략 구현 (무필터 vs 필터)

### Phase 3: 서비스 적용 (순차)
7. Count 서비스 필터 적용 (PoC)
8. Dynamic 서비스 필터 적용
9. Grid 서비스 필터 적용
10. Static 서비스 필터 적용 (필요 시)

### Phase 4: 최적화
11. 캐시 워밍업 스크립트
12. 성능 모니터링 및 튜닝
13. 필요 시 Caffeine 1차 캐시 추가

---

## 9. 결론

### 9.1 기술적 실현 가능성
**가능함.** H3 기반 공간 인덱싱 + 해시 기반 캐시 전략으로 ES 없이도 필터링된 클러스터링 구현 가능.

### 9.2 권장 사항
- B2B 저트래픽 서비스에서 ES는 오버스펙
- Redis 캐시 기반 방식이 운영 복잡도/비용 면에서 유리
- 콜드 부트 이슈는 워밍업으로 완화 가능
- 추후 트래픽 증가 시 Redis 스케일업 또는 Caffeine 1차 캐시 추가

### 9.3 예상 효과
- **캐시 히트 시**: ES 대비 동등 또는 더 빠른 응답 (< 10ms)
- **운영 비용**: ES 클러스터 대비 70% 이상 절감
- **운영 복잡도**: 기존 Redis 인프라 활용으로 추가 러닝커브 없음

---

## 부록: 관련 DDL

### 소스 테이블 (land_characteristic)
```sql
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
```

### PNU-H3 인덱싱 테이블
```sql
CREATE TABLE manage.r3_pnu_h3_10 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);

CREATE TABLE manage.r3_pnu_h3_7 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);

CREATE TABLE manage.r3_pnu_h3_5 (
    pnu text NOT NULL,
    h3_index bigint NOT NULL,
    PRIMARY KEY (h3_index, pnu)
);
```

---

*작성일: 2026-01-16*
*검토 요청: 아키텍처 타당성, 성능 예상치, 리스크 평가*
