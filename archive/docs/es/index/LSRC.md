# LSRC (Land Static Region Cluster)
고정형 행정구역 클러스터 - 비즈니스 필터 없이 전체 필지를 행정구역 단위로 집계

## 인덱스 정보
| key        | value                        |
|------------|------------------------------|
| IndexName  | `land_static_region_cluster` |
| Shards     | 1                            |
| Replicas   | 0                            |

### 매핑
level: SD, SGG, EMD
code: 10자리 (단순히 boundary_region과 통일)
```json
{
  "level": "keyword",
  "code": "keyword",
  "name": "keyword",
  "count": "integer",
  "center": "geo_point"
}
```

## API 엔드포인트
| Method | Endpoint                  |
|--------|---------------------------|
| PUT    | `/api/es/lsrc/reindex`    | 전체 재인덱싱 
| PUT    | `/api/es/lsrc/forcemerge` | 세그먼트 병합
| GET    | `/api/es/lsrc/count`      | 인덱스 문서 수
| DELETE | `/api/es/lsrc`            | 인덱스 삭제

## Reindex 처리 순서
```
[1] ensureIndexExists()
    ├─ 기존 인덱스 존재 시 삭제
    └─ 신규 인덱스 생성 (shard:1, replica:0)
           ↓
[2] DB 한 방 집계
    SELECT LEFT(bjdong_cd, 8), COUNT(*), SUM(...), SUM(...)
    FROM land_characteristic
    WHERE center IS NOT NULL
    GROUP BY LEFT(bjdong_cd, 8)
    → 결과: ~5,000개 EMD 집계 row
           ↓
[3] 앱에서 롤업 (EMD → SGG → SD)
    각 EMD row를 순회하며:
    - emdMap에 누적
    - sggMap에 누적 (앞 5자리)
    - sdMap에 누적 (앞 2자리)
           ↓
[4] 행정구역 이름 로드
    boundaryRegionRepo.findAll() → regionCode:name 맵
           ↓
[5] Document 변환
    - SD 문서 생성 (시도) ~17개
    - SGG 문서 생성 (시군구) ~250개
    - EMD 문서 생성 (읍면동) ~5,000개
    각 문서: { level, code, name, count, center(avg lat/lng) }
           ↓
[6] ES Bulk 인덱싱
    전체 ~5,000개 정도의 문서, ~800KB → 한 번에 처리 (ES 권장 5-15MB 대비 충분히 작음)
           ↓
[7] Forcemerge
    maxNumSegments(1) → 세그먼트 병합
           ↓
[8] 결과 반환
    { sd, sgg, emd, total, dbElapsedMs, elapsedMs, success }
```

## 핵심 쿼리
```sql
-- 전체 EMD 집계 (한 방 쿼리)
SELECT
    LEFT(bjdong_cd, 8) as code,
    COUNT(*) as cnt,
    SUM(ST_Y(center)) as sum_lat,
    SUM(ST_X(center)) as sum_lng
FROM external_data.land_characteristic
WHERE center IS NOT NULL
GROUP BY LEFT(bjdong_cd, 8);
```

**인덱스 활용:**
- `idx_lc_bjdong_cd_left8` 인덱스 사용
- DB가 GROUP BY 최적화

## 앱 롤업 로직
```kotlin
// DB 집계 결과를 순회하며 롤업
for (row in aggResults) {
    val code = row.code      // 8자리
    val cnt = row.cnt
    val sumLat = row.sumLat
    val sumLng = row.sumLng

    // EMD (8자리 → 10자리)
    val emdCode = code.padEnd(10, '0')
    emdAgg[emdCode].add(cnt, sumLat, sumLng)

    // SGG (5자리 → 10자리)
    val sggCode = code.substring(0, 5).padEnd(10, '0')
    sggAgg[sggCode].add(cnt, sumLat, sumLng)

    // SD (2자리 → 10자리)
    val sdCode = code.substring(0, 2).padEnd(10, '0')
    sdAgg[sdCode].add(cnt, sumLat, sumLng)
}
```



### Bulk Size 분석

**문서 크기:**
- LsrcDocument ≈ 100-150 bytes (id, level, code, name, count, center)
- 총 문서 수: ~5,000개 정도 (SD 17 + SGG 250 + EMD 5,000)
- 전체 페이로드: ~800KB

**ES 권장사항:** 5-15MB per bulk request

**결론:** 전체 문서가 1MB 미만이므로 청크 분할 불필요. 한 방에 처리해도 무방.
현재 2000은 보수적 설정이며, 5000 이상으로 올리거나 전체 한번에 처리 가능.

## 데이터 흐름

```
PostgreSQL (land_characteristic)
    │
    │  DB GROUP BY 집계 (쿼리 1번)
    ↓
EMD 집계 결과 (~3,500 row)
    │
    │  앱에서 SGG/SD 롤업
    ↓
집계 결과
    │  - sdMap: ~17개
    │  - sggMap: ~250개
    │  - emdMap: ~3,500개
    ↓
LsrcDocument 리스트 (~3,767개)
    │
    │  bulk indexing
    ↓
Elasticsearch (lsrc 인덱스)
```

## 행정구역 코드 규칙
→ [skills/BUSINESS_INFO.md](../skills/BUSINESS_INFO.md) 참조
**현재 구현:**
- DB에서 8자리(EMD)까지만 집계 (`LEFT(bjdong_cd, 8)`)
- 리 단위는 집계하지 않음 - EMD로 통합

## 관련 파일
- Controller: `controller/rest/LsrcRestController.kt`
- Service: `es/service/LsrcIndexingService.kt`
- Document: `es/document/cluster/LsrcDocument.kt`
- Repository: `jpa/repository/LandCharacteristicRepository.kt`
