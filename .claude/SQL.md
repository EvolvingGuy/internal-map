# 이하는 소스를 통합 버전 테이블
## land_master (컬럼 목록과 타입 설명과 특이사항 및 변경점. 명시되지 않으면 그대로. 타입이 제대로 변경되지 않으면 null 처리)
### land_characteristic
- bjdong_cd (BIGINT로 타입 변경)
- area (double)
- price (double)
- crtn_day (date로 변경. YYYYMMDD 포맷/윤년/월말일 유효성 철저 체크. 실패시 null)

### building_ledger_outline_summaries
#### 고려사항
- 조인 조건: `l.pnu = (b.sigungu_cd || b.bjdong_cd || (CASE WHEN b.plat_gb_cd = '0' THEN '1' ELSE '2' END) || b.bun || b.ji)`
  - bun, ji는 4자리 0패딩 완료됨
  - plat_gb_cd 매핑 (Building → PNU): '0'→'1'(대지), '1'→'2'(산)
- pnu마다 여러개 가능 → `ROW_NUMBER() OVER (PARTITION BY pnu ORDER BY create_dt DESC)` 로 마지막 것 선택
- 타입 변경 (실패시 null):
  - pms_day, stcns_day, use_apr_day → date (YYYYMMDD 유효성 체크)
  - tot_area, plat_area, arch_area → double
- 추가 컬럼 (summaries 전용): new_regstr_gb_cd, new_regstr_gb_cd_nm, main_bld_cnt, tot_pkng_cnt

### building_ledger_outline
#### 고려사항
- summaries와 겹치지 않는 컬럼만 추가 (summaries UPDATE 후 실행)
- `mgm_bldrgst_pk IS NULL`인 행에 대해서만 UPDATE
- 조인 조건: summaries와 동일
- pnu마다 여러개 가능 → `ROW_NUMBER() OVER (PARTITION BY pnu ORDER BY create_dt DESC)` 로 마지막 것 선택
- 타입 변경 (실패시 null):
  - pms_day, stcns_day, use_apr_day → dateㅓ야
  - tot_area, plat_area, arch_area → double
- outline 전용 컬럼:
  - dong_nm
  - main_atch_gb_cd, main_atch_gb_cd_nm
  - strct_cd, strct_cd_nm, etc_strct
  - roof_cd, roof_cd_nm, etc_roof
  - heit (double)
  - grnd_flr_cnt, ugrnd_flr_cnt (int)
  - ride_use_elvt_cnt, emgen_use_elvt_cnt (int)
  - tot_dong_tot_area (double)
  - rserthqk_dsgn_apply_yn, rserthqk_ablty


```sql
-- land_master 파티션 테이블 DDL (sd 기준 LIST 파티셔닝)
-- land_characteristic 컬럼만 포함
CREATE TABLE manage.land_master (
    sd int NOT NULL,
    pnu text NOT NULL,
    bjdong_cd bigint NULL,
    bjdong_nm text NULL,
    regstr_gb_cd text NULL,
    regstr_gb text NULL,
    jibun text NULL,
    jimok_sign text NULL,
    std_year text NULL,
    std_month text NULL,
    jimok_cd text NULL,
    jimok text NULL,
    area double precision NULL,
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
    price double precision NULL,
    crtn_day date NULL,
    geometry geometry(Geometry, 4326) NULL,
    center geometry(Point, 4326) NULL,
    center_lat double precision NULL,
    center_lng double precision NULL,
    is_donut bool NULL,
    create_dt timestamp NULL,
    update_dt timestamp NULL,
    PRIMARY KEY (sd, pnu)
) PARTITION BY LIST (sd);

-- 파티션 생성 (시도별)
CREATE TABLE manage.land_master_sd_11 PARTITION OF manage.land_master FOR VALUES IN (11); -- 서울 903,166
CREATE TABLE manage.land_master_sd_26 PARTITION OF manage.land_master FOR VALUES IN (26); -- 부산 714,166
CREATE TABLE manage.land_master_sd_27 PARTITION OF manage.land_master FOR VALUES IN (27); -- 대구 791,389
CREATE TABLE manage.land_master_sd_28 PARTITION OF manage.land_master FOR VALUES IN (28); -- 인천 669,158
CREATE TABLE manage.land_master_sd_29 PARTITION OF manage.land_master FOR VALUES IN (29); -- 광주 390,256
CREATE TABLE manage.land_master_sd_30 PARTITION OF manage.land_master FOR VALUES IN (30); -- 대전 292,049
CREATE TABLE manage.land_master_sd_31 PARTITION OF manage.land_master FOR VALUES IN (31); -- 울산 507,628
CREATE TABLE manage.land_master_sd_36 PARTITION OF manage.land_master FOR VALUES IN (36); -- 세종 206,484
CREATE TABLE manage.land_master_sd_41 PARTITION OF manage.land_master FOR VALUES IN (41); -- 경기 5,164,227
CREATE TABLE manage.land_master_sd_43 PARTITION OF manage.land_master FOR VALUES IN (43); -- 충북 2,395,086
CREATE TABLE manage.land_master_sd_44 PARTITION OF manage.land_master FOR VALUES IN (44); -- 충남 3,740,187
CREATE TABLE manage.land_master_sd_46 PARTITION OF manage.land_master FOR VALUES IN (46); -- 전남 5,899,035
CREATE TABLE manage.land_master_sd_47 PARTITION OF manage.land_master FOR VALUES IN (47); -- 경북 5,694,523
CREATE TABLE manage.land_master_sd_48 PARTITION OF manage.land_master FOR VALUES IN (48); -- 경남 4,819,948
CREATE TABLE manage.land_master_sd_50 PARTITION OF manage.land_master FOR VALUES IN (50); -- 제주 880,280
CREATE TABLE manage.land_master_sd_51 PARTITION OF manage.land_master FOR VALUES IN (51); -- 강원 2,729,990
CREATE TABLE manage.land_master_sd_52 PARTITION OF manage.land_master FOR VALUES IN (52); -- 전북 3,870,798

INSERT INTO manage.land_master (sd, pnu, bjdong_cd, bjdong_nm, regstr_gb_cd, regstr_gb, jibun,
                                jimok_sign, std_year, std_month, jimok_cd, jimok, area,
                                jiyuk_cd_1, jiyuk_1, jiyuk_cd_2, jiyuk_2,
                                land_use_cd, land_use, height_cd, height, shape_cd, shape,
                                road_cd, road, price, crtn_day, geometry, center,
                                center_lat, center_lng, is_donut, create_dt)
SELECT LEFT(pnu, 2)::int,
       pnu,
       NULLIF(bjdong_cd, '')::bigint,
       bjdong_nm,
       regstr_gb_cd,
       regstr_gb,
       jibun,
       jimok_sign,
       std_year,
       std_month,
       jimok_cd,
       jimok,
       NULLIF(area, '')::double precision,
       jiyuk_cd_1,
       jiyuk_1,
       jiyuk_cd_2,
       jiyuk_2,
       land_use_cd,
       land_use,
       height_cd,
       height,
       shape_cd,
       shape,
       road_cd,
       road,
       NULLIF(price, '')::double precision,
       CASE
         WHEN crtn_day IS NULL OR crtn_day = '' THEN NULL
         WHEN crtn_day !~ '^\d{8}$' THEN NULL
         WHEN SUBSTRING(crtn_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
         WHEN SUBSTRING(crtn_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
         WHEN SUBSTRING(crtn_day, 5, 2) IN ('04', '06', '09', '11')
           AND SUBSTRING(crtn_day, 7, 2)::int > 30 THEN NULL
         WHEN SUBSTRING(crtn_day, 5, 2) = '02'
           AND SUBSTRING(crtn_day, 7, 2)::int > 29 THEN NULL
         WHEN SUBSTRING(crtn_day, 5, 2) = '02'
           AND SUBSTRING(crtn_day, 7, 2)::int = 29
           AND NOT (
             SUBSTRING(crtn_day, 1, 4)::int % 4 = 0
               AND (SUBSTRING(crtn_day, 1, 4)::int % 100 != 0
               OR SUBSTRING(crtn_day, 1, 4)::int % 400 = 0)
             ) THEN NULL
         ELSE TO_DATE(crtn_day, 'YYYYMMDD')
         END,
       geometry,
       center,
       ST_Y(center),
       ST_X(center),
       is_donut,
       create_dt
FROM external_data.land_characteristic
WHERE pnu IS NOT NULL;

-- 건축물대장 총괄표제부
ALTER TABLE manage.land_master
  ADD COLUMN mgm_bldrgst_pk text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN bld_regstr_gb_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN bld_regstr_gb_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN regstr_kind_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN regstr_kind_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN new_regstr_gb_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN new_regstr_gb_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN plat_plc text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN new_plat_plc text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN bld_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN plat_area double precision NULL;
ALTER TABLE manage.land_master
  ADD COLUMN arch_area double precision NULL;
ALTER TABLE manage.land_master
  ADD COLUMN bc_rat text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN tot_area double precision NULL;
ALTER TABLE manage.land_master
  ADD COLUMN vl_rat_estm_tot_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN vl_rat text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN main_purps_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN main_purps_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN etc_purps text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN hhld_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN fmly_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN main_bld_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN atch_bld_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN atch_bld_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN tot_pkng_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN indr_mech_ut_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN indr_mech_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN oudr_mech_ut_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN oudr_mech_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN indr_auto_ut_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN indr_auto_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN oudr_auto_ut_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN oudr_auto_area text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pms_day date NULL;
ALTER TABLE manage.land_master
  ADD COLUMN stcns_day date NULL;
ALTER TABLE manage.land_master
  ADD COLUMN use_apr_day date NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pmsno_year text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pmsno_kik_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pmsno_kik_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pmsno_gb_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN pmsno_gb_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN ho_cnt text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN engr_grade text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN engr_rat text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN engr_epi text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN gn_bld_grade text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN gn_bld_cert text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN itg_bld_grade text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN itg_bld_cert text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN bld_crtn_day text NULL;


WITH ranked_summaries
       AS (SELECT (sigungu_cd || bjdong_cd || (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) || bun ||
                   ji)  AS derived_pnu,
                  ROW_NUMBER() OVER (
                    PARTITION BY (
                      sigungu_cd || bjdong_cd || (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) || bun || ji)
                    ORDER BY create_dt DESC
                    ) AS rn
           FROM external_data.building_ledger_outline_summaries)
SELECT COUNT(*)                AS total_unique_pnu,
       COUNT(l.pnu)            AS matched,
       COUNT(*) - COUNT(l.pnu) AS not_matched
FROM ranked_summaries s
       LEFT JOIN manage.land_master l
                 ON l.sd = LEFT(s.derived_pnu, 2)::int
                   AND l.pnu = s.derived_pnu
WHERE s.rn = 1;

-- ========================================                                                                                                                                                      
-- 2. UPDATE: building_ledger_outline_summaries → land_master                                                                                                                                    
-- ========================================                                                                                                                                                      
WITH ranked_summaries AS (SELECT *,
                                 (sigungu_cd || bjdong_cd || (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) ||
                                  bun || ji) AS derived_pnu,
                                 ROW_NUMBER() OVER (
                                   PARTITION BY (sigungu_cd || bjdong_cd ||
                                                 (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) || bun || ji)
                                   ORDER BY create_dt DESC
                                   )       AS rn
                          FROM external_data.building_ledger_outline_summaries)
UPDATE manage.land_master l
SET mgm_bldrgst_pk       = s.mgm_bldrgst_pk,
    bld_regstr_gb_cd     = s.regstr_gb_cd,
    bld_regstr_gb_cd_nm  = s.regstr_gb_cd_nm,
    regstr_kind_cd       = s.regstr_kind_cd,
    regstr_kind_cd_nm    = s.regstr_kind_cd_nm,
    new_regstr_gb_cd     = s.new_regstr_gb_cd,
    new_regstr_gb_cd_nm  = s.new_regstr_gb_cd_nm,
    plat_plc             = s.plat_plc,
    new_plat_plc         = s.new_plat_plc,
    bld_nm               = s.bld_nm,
    plat_area            = NULLIF(s.plat_area, '')::double precision,
    arch_area            = NULLIF(s.arch_area, '')::double precision,
    bc_rat               = s.bc_rat,
    tot_area             = NULLIF(s.tot_area, '')::double precision,
    vl_rat_estm_tot_area = s.vl_rat_estm_tot_area,
    vl_rat               = s.vl_rat,
    main_purps_cd        = s.main_purps_cd,
    main_purps_cd_nm     = s.main_purps_cd_nm,
    etc_purps            = s.etc_purps,
    hhld_cnt             = s.hhld_cnt,
    fmly_cnt             = s.fmly_cnt,
    main_bld_cnt         = s.main_bld_cnt,
    atch_bld_cnt         = s.atch_bld_cnt,
    atch_bld_area        = s.atch_bld_area,
    tot_pkng_cnt         = s.tot_pkng_cnt,
    indr_mech_ut_cnt     = s.indr_mech_ut_cnt,
    indr_mech_area       = s.indr_mech_area,
    oudr_mech_ut_cnt     = s.oudr_mech_ut_cnt,
    oudr_mech_area       = s.oudr_mech_area,
    indr_auto_ut_cnt     = s.indr_auto_ut_cnt,
    indr_auto_area       = s.indr_auto_area,
    oudr_auto_ut_cnt     = s.oudr_auto_ut_cnt,
    oudr_auto_area       = s.oudr_auto_area,
    pms_day              = CASE
                             WHEN s.pms_day IS NULL OR s.pms_day = '' THEN NULL
                             WHEN s.pms_day !~ '^\d{8}$' THEN NULL
                             WHEN SUBSTRING(s.pms_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                             WHEN SUBSTRING(s.pms_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                             WHEN SUBSTRING(s.pms_day, 5, 2) IN ('04', '06', '09', '11')
                               AND SUBSTRING(s.pms_day, 7, 2)::int > 30 THEN NULL
                             WHEN SUBSTRING(s.pms_day, 5, 2) = '02'
                               AND SUBSTRING(s.pms_day, 7, 2)::int > 29 THEN NULL
                             WHEN SUBSTRING(s.pms_day, 5, 2) = '02'
                               AND SUBSTRING(s.pms_day, 7, 2)::int = 29
                               AND NOT (
                                 SUBSTRING(s.pms_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(s.pms_day, 1, 4)::int % 100 != 0
                                   OR SUBSTRING(s.pms_day, 1, 4)::int % 400 = 0)
                                 ) THEN NULL
                             ELSE TO_DATE(s.pms_day, 'YYYYMMDD')
      END,
    stcns_day            = CASE
                             WHEN s.stcns_day IS NULL OR s.stcns_day = '' THEN NULL
                             WHEN s.stcns_day !~ '^\d{8}$' THEN NULL
                             WHEN SUBSTRING(s.stcns_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                             WHEN SUBSTRING(s.stcns_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                             WHEN SUBSTRING(s.stcns_day, 5, 2) IN ('04', '06', '09', '11')
                               AND SUBSTRING(s.stcns_day, 7, 2)::int > 30 THEN NULL
                             WHEN SUBSTRING(s.stcns_day, 5, 2) = '02'
                               AND SUBSTRING(s.stcns_day, 7, 2)::int > 29 THEN NULL
                             WHEN SUBSTRING(s.stcns_day, 5, 2) = '02'
                               AND SUBSTRING(s.stcns_day, 7, 2)::int = 29
                               AND NOT (
                                 SUBSTRING(s.stcns_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(s.stcns_day, 1, 4)::int % 100 != 0
                                   OR SUBSTRING(s.stcns_day, 1, 4)::int % 400 = 0)
                                 ) THEN NULL
                             ELSE TO_DATE(s.stcns_day, 'YYYYMMDD')
      END,
    use_apr_day          = CASE
                             WHEN s.use_apr_day IS NULL OR s.use_apr_day = '' THEN NULL
                             WHEN s.use_apr_day !~ '^\d{8}$' THEN NULL
                             WHEN SUBSTRING(s.use_apr_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                             WHEN SUBSTRING(s.use_apr_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                             WHEN SUBSTRING(s.use_apr_day, 5, 2) IN ('04', '06', '09', '11')
                               AND SUBSTRING(s.use_apr_day, 7, 2)::int > 30 THEN NULL
                             WHEN SUBSTRING(s.use_apr_day, 5, 2) = '02'
                               AND SUBSTRING(s.use_apr_day, 7, 2)::int > 29 THEN NULL
                             WHEN SUBSTRING(s.use_apr_day, 5, 2) = '02'
                               AND SUBSTRING(s.use_apr_day, 7, 2)::int = 29
                               AND NOT (
                                 SUBSTRING(s.use_apr_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(s.use_apr_day, 1, 4)::int % 100 != 0
                                   OR SUBSTRING(s.use_apr_day, 1, 4)::int % 400 = 0)
                                 ) THEN NULL
                             ELSE TO_DATE(s.use_apr_day, 'YYYYMMDD')
      END,
    pmsno_year           = s.pmsno_year,
    pmsno_kik_cd         = s.pmsno_kik_cd,
    pmsno_kik_cd_nm      = s.pmsno_kik_cd_nm,
    pmsno_gb_cd          = s.pmsno_gb_cd,
    pmsno_gb_cd_nm       = s.pmsno_gb_cd_nm,
    ho_cnt               = s.ho_cnt,
    engr_grade           = s.engr_grade,
    engr_rat             = s.engr_rat,
    engr_epi             = s.engr_epi,
    gn_bld_grade         = s.gn_bld_grade,
    gn_bld_cert          = s.gn_bld_cert,
    itg_bld_grade        = s.itg_bld_grade,
    itg_bld_cert         = s.itg_bld_cert,
    bld_crtn_day         = s.crtn_day,
    update_dt            = NOW()
FROM ranked_summaries s
WHERE l.sd = LEFT(s.derived_pnu, 2)::int
  AND l.pnu = s.derived_pnu
  AND s.rn = 1;

-- 건축물대장 일반건축물/표제부 컬럼은 총괄표제부에 없는 것만 추가
ALTER TABLE manage.land_master
  ADD COLUMN dong_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN main_atch_gb_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN main_atch_gb_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN strct_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN strct_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN etc_strct text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN roof_cd text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN roof_cd_nm text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN etc_roof text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN heit double precision NULL;
ALTER TABLE manage.land_master
  ADD COLUMN grnd_flr_cnt int NULL;
ALTER TABLE manage.land_master
  ADD COLUMN ugrnd_flr_cnt int NULL;
ALTER TABLE manage.land_master
  ADD COLUMN ride_use_elvt_cnt int NULL;
ALTER TABLE manage.land_master
  ADD COLUMN emgen_use_elvt_cnt int NULL;
ALTER TABLE manage.land_master
  ADD COLUMN tot_dong_tot_area double precision NULL;
ALTER TABLE manage.land_master
  ADD COLUMN rserthqk_dsgn_apply_yn text NULL;
ALTER TABLE manage.land_master
  ADD COLUMN rserthqk_ablty text NULL;

WITH ranked_outline AS (SELECT *,
                               (sigungu_cd || bjdong_cd || (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) || bun ||
                                ji)  AS derived_pnu,
                               ROW_NUMBER() OVER (
                                 PARTITION BY (
                                   sigungu_cd || bjdong_cd || (CASE WHEN plat_gb_cd = '0' THEN '1' ELSE '2' END) ||
                                   bun || ji)
                                 ORDER BY create_dt DESC
                                 ) AS rn
                        FROM external_data.building_ledger_outline)
UPDATE manage.land_master l
SET mgm_bldrgst_pk         = o.mgm_bldrgst_pk,
    bld_regstr_gb_cd       = o.regstr_gb_cd,
    bld_regstr_gb_cd_nm    = o.regstr_gb_cd_nm,
    regstr_kind_cd         = o.regstr_kind_cd,
    regstr_kind_cd_nm      = o.regstr_kind_cd_nm,
    plat_plc               = o.plat_plc,
    new_plat_plc           = o.new_plat_plc,
    bld_nm                 = o.bld_nm,
    plat_area              = CASE
                               WHEN o.plat_area IS NULL OR o.plat_area = '' THEN NULL
                               WHEN o.plat_area !~ '^[0-9]+\.?[0-9]*$' THEN NULL
                               ELSE o.plat_area::double precision END,
    arch_area              = CASE
                               WHEN o.arch_area IS NULL OR o.arch_area = '' THEN NULL
                               WHEN o.arch_area !~ '^[0-9]+\.?[0-9]*$' THEN NULL
                               ELSE o.arch_area::double precision END,
    bc_rat                 = o.bc_rat,
    tot_area               = CASE
                               WHEN o.tot_area IS NULL OR o.tot_area = '' THEN NULL
                               WHEN o.tot_area !~ '^[0-9]+\.?[0-9]*$' THEN NULL
                               ELSE o.tot_area::double precision END,
    vl_rat_estm_tot_area   = o.vl_rat_estm_tot_area,
    vl_rat                 = o.vl_rat,
    main_purps_cd          = o.main_purps_cd,
    main_purps_cd_nm       = o.main_purps_cd_nm,
    etc_purps              = o.etc_purps,
    hhld_cnt               = o.hhld_cnt,
    fmly_cnt               = o.fmly_cnt,
    atch_bld_cnt           = o.atch_bld_cnt,
    atch_bld_area          = o.atch_bld_area,
    indr_mech_ut_cnt       = o.indr_mech_ut_cnt,
    indr_mech_area         = o.indr_mech_area,
    oudr_mech_ut_cnt       = o.oudr_mech_ut_cnt,
    oudr_mech_area         = o.oudr_mech_area,
    indr_auto_ut_cnt       = o.indr_auto_ut_cnt,
    indr_auto_area         = o.indr_auto_area,
    oudr_auto_ut_cnt       = o.oudr_auto_ut_cnt,
    oudr_auto_area         = o.oudr_auto_area,
    pms_day                = CASE
                               WHEN o.pms_day IS NULL OR o.pms_day = '' THEN NULL
                               WHEN o.pms_day !~ '^\d{8}$' THEN NULL
                               WHEN SUBSTRING(o.pms_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                               WHEN SUBSTRING(o.pms_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                               WHEN SUBSTRING(o.pms_day, 5, 2) IN ('04', '06', '09', '11') AND
                                    SUBSTRING(o.pms_day, 7, 2)::int > 30 THEN NULL
                               WHEN SUBSTRING(o.pms_day, 5, 2) = '02' AND SUBSTRING(o.pms_day, 7, 2)::int > 29
                                 THEN NULL
                               WHEN SUBSTRING(o.pms_day, 5, 2) = '02' AND SUBSTRING(o.pms_day, 7, 2)::int = 29
                                 AND NOT (SUBSTRING(o.pms_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(o.pms_day, 1, 4)::int % 100 != 0 OR
                                        SUBSTRING(o.pms_day, 1, 4)::int % 400 = 0)) THEN NULL
                               ELSE TO_DATE(o.pms_day, 'YYYYMMDD')
      END,
    stcns_day              = CASE
                               WHEN o.stcns_day IS NULL OR o.stcns_day = '' THEN NULL
                               WHEN o.stcns_day !~ '^\d{8}$' THEN NULL
                               WHEN SUBSTRING(o.stcns_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                               WHEN SUBSTRING(o.stcns_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                               WHEN SUBSTRING(o.stcns_day, 5, 2) IN ('04', '06', '09', '11') AND
                                    SUBSTRING(o.stcns_day, 7, 2)::int > 30 THEN NULL
                               WHEN SUBSTRING(o.stcns_day, 5, 2) = '02' AND SUBSTRING(o.stcns_day, 7, 2)::int > 29
                                 THEN NULL
                               WHEN SUBSTRING(o.stcns_day, 5, 2) = '02' AND SUBSTRING(o.stcns_day, 7, 2)::int = 29
                                 AND NOT (SUBSTRING(o.stcns_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(o.stcns_day, 1, 4)::int % 100 != 0 OR
                                        SUBSTRING(o.stcns_day, 1, 4)::int % 400 = 0)) THEN NULL
                               ELSE TO_DATE(o.stcns_day, 'YYYYMMDD')
      END,
    use_apr_day            = CASE
                               WHEN o.use_apr_day IS NULL OR o.use_apr_day = '' THEN NULL
                               WHEN o.use_apr_day !~ '^\d{8}$' THEN NULL
                               WHEN SUBSTRING(o.use_apr_day, 5, 2)::int NOT BETWEEN 1 AND 12 THEN NULL
                               WHEN SUBSTRING(o.use_apr_day, 7, 2)::int NOT BETWEEN 1 AND 31 THEN NULL
                               WHEN SUBSTRING(o.use_apr_day, 5, 2) IN ('04', '06', '09', '11') AND
                                    SUBSTRING(o.use_apr_day, 7, 2)::int > 30 THEN NULL
                               WHEN SUBSTRING(o.use_apr_day, 5, 2) = '02' AND SUBSTRING(o.use_apr_day, 7, 2)::int > 29
                                 THEN NULL
                               WHEN SUBSTRING(o.use_apr_day, 5, 2) = '02' AND SUBSTRING(o.use_apr_day, 7, 2)::int = 29
                                 AND NOT (SUBSTRING(o.use_apr_day, 1, 4)::int % 4 = 0
                                   AND (SUBSTRING(o.use_apr_day, 1, 4)::int % 100 != 0 OR
                                        SUBSTRING(o.use_apr_day, 1, 4)::int % 400 = 0)) THEN NULL
                               ELSE TO_DATE(o.use_apr_day, 'YYYYMMDD')
      END,
    pmsno_year             = o.pmsno_year,
    pmsno_kik_cd           = o.pmsno_kik_cd,
    pmsno_kik_cd_nm        = o.pmsno_kik_cd_nm,
    pmsno_gb_cd            = o.pmsno_gb_cd,
    pmsno_gb_cd_nm         = o.pmsno_gb_cd_nm,
    ho_cnt                 = o.ho_cnt,
    engr_grade             = o.engr_grade,
    engr_rat               = o.engr_rat,
    engr_epi               = o.engr_epi,
    gn_bld_grade           = o.gn_bld_grade,
    gn_bld_cert            = o.gn_bld_cert,
    itg_bld_grade          = o.itg_bld_grade,
    itg_bld_cert           = o.itg_bld_cert,
    bld_crtn_day           = o.crtn_day,
    -- outline 전용                                                                                                                                                                              
    dong_nm                = o.dong_nm,
    main_atch_gb_cd        = o.main_atch_gb_cd,
    main_atch_gb_cd_nm     = o.main_atch_gb_cd_nm,
    strct_cd               = o.strct_cd,
    strct_cd_nm            = o.strct_cd_nm,
    etc_strct              = o.etc_strct,
    roof_cd                = o.roof_cd,
    roof_cd_nm             = o.roof_cd_nm,
    etc_roof               = o.etc_roof,
    heit                   = CASE
                               WHEN o.heit IS NULL OR o.heit = '' THEN NULL
                               WHEN o.heit !~ '^-?[0-9]+\.?[0-9]*$' THEN NULL
                               ELSE o.heit::double precision END,
    grnd_flr_cnt           = CASE
                               WHEN o.grnd_flr_cnt IS NULL OR o.grnd_flr_cnt = '' THEN NULL
                               WHEN o.grnd_flr_cnt !~ '^-?[0-9]+$' THEN NULL
                               ELSE o.grnd_flr_cnt::int END,
    ugrnd_flr_cnt          = CASE
                               WHEN o.ugrnd_flr_cnt IS NULL OR o.ugrnd_flr_cnt = '' THEN NULL
                               WHEN o.ugrnd_flr_cnt !~ '^-?[0-9]+$' THEN NULL
                               ELSE o.ugrnd_flr_cnt::int END,
    ride_use_elvt_cnt      = CASE
                               WHEN o.ride_use_elvt_cnt IS NULL OR o.ride_use_elvt_cnt = '' THEN NULL
                               WHEN o.ride_use_elvt_cnt !~ '^[0-9]+$' THEN NULL
                               ELSE o.ride_use_elvt_cnt::int END,
    emgen_use_elvt_cnt     = CASE
                               WHEN o.emgen_use_elvt_cnt IS NULL OR o.emgen_use_elvt_cnt = '' THEN NULL
                               WHEN o.emgen_use_elvt_cnt !~ '^[0-9]+$' THEN NULL
                               ELSE o.emgen_use_elvt_cnt::int END,
    tot_dong_tot_area      = CASE
                               WHEN o.tot_dong_tot_area IS NULL OR o.tot_dong_tot_area = '' THEN NULL
                               WHEN o.tot_dong_tot_area !~ '^[0-9]+\.?[0-9]*$' THEN NULL
                               ELSE o.tot_dong_tot_area::double precision END,
    rserthqk_dsgn_apply_yn = o.rserthqk_dsgn_apply_yn,
    rserthqk_ablty         = o.rserthqk_ablty,
    update_dt              = NOW()
FROM ranked_outline o
WHERE l.sd = LEFT(o.derived_pnu, 2)::int
  AND l.pnu = o.derived_pnu
  AND o.rn = 1
  AND l.mgm_bldrgst_pk IS NULL;      
```

-- 이하는 소스를 통한 실거래 분리 버전 테이블

-- 이하는 소스 테이블
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
CREATE INDEX idx_lc_bjdong_cd_left2 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 2));
CREATE INDEX idx_lc_bjdong_cd_left5 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 5));
CREATE INDEX idx_lc_bjdong_cd_left8 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 8));
CREATE INDEX land_characteristic_bjdong_cd_index ON external_data.land_characteristic USING btree (bjdong_cd);
CREATE INDEX land_characteristic_pnu_index ON external_data.land_characteristic USING btree (pnu);

-- external_data.building_ledger_outline definition

인덱스 기준으로 어떤 pnu가 있는지


-- Drop table

-- DROP TABLE external_data.building_ledger_outline;

CREATE TABLE external_data.building_ledger_outline (
mgm_bldrgst_pk text NULL,
regstr_gb_cd text NULL,
regstr_gb_cd_nm text NULL,
regstr_kind_cd text NULL,
regstr_kind_cd_nm text NULL,
plat_plc text NULL,
new_plat_plc text NULL,
bld_nm text NULL,
sigungu_cd text NULL,
bjdong_cd text NULL,
plat_gb_cd text NULL,
bun text NULL,
ji text NULL,
splot_nm text NULL,
block text NULL,
lot text NULL,
bylot_cnt text NULL,
na_road_cd text NULL,
na_bjdong_cd text NULL,
na_ugrnd_cd text NULL,
na_main_bun text NULL,
na_sub_bun text NULL,
dong_nm text NULL,
main_atch_gb_cd text NULL,
main_atch_gb_cd_nm text NULL,
plat_area text NULL,
arch_area text NULL,
bc_rat text NULL,
tot_area text NULL,
vl_rat_estm_tot_area text NULL,
vl_rat text NULL,
strct_cd text NULL,
strct_cd_nm text NULL,
etc_strct text NULL,
main_purps_cd text NULL,
main_purps_cd_nm text NULL,
etc_purps text NULL,
roof_cd text NULL,
roof_cd_nm text NULL,
etc_roof text NULL,
hhld_cnt text NULL,
fmly_cnt text NULL,
heit text NULL,
grnd_flr_cnt text NULL,
ugrnd_flr_cnt text NULL,
ride_use_elvt_cnt text NULL,
emgen_use_elvt_cnt text NULL,
atch_bld_cnt text NULL,
atch_bld_area text NULL,
tot_dong_tot_area text NULL,
indr_mech_ut_cnt text NULL,
indr_mech_area text NULL,
oudr_mech_ut_cnt text NULL,
oudr_mech_area text NULL,
indr_auto_ut_cnt text NULL,
indr_auto_area text NULL,
oudr_auto_ut_cnt text NULL,
oudr_auto_area text NULL,
pms_day text NULL,
stcns_day text NULL,
use_apr_day text NULL,
pmsno_year text NULL,
pmsno_kik_cd text NULL,
pmsno_kik_cd_nm text NULL,
pmsno_gb_cd text NULL,
pmsno_gb_cd_nm text NULL,
ho_cnt text NULL,
engr_grade text NULL,
engr_rat text NULL,
engr_epi text NULL,
gn_bld_grade text NULL,
gn_bld_cert text NULL,
itg_bld_grade text NULL,
itg_bld_cert text NULL,
crtn_day text NULL,
rserthqk_dsgn_apply_yn text NULL,
rserthqk_ablty text NULL,
create_dt timestamptz NULL
);

-- external_data.building_ledger_outline_summaries definition

-- Drop table

-- DROP TABLE external_data.building_ledger_outline_summaries;

CREATE TABLE external_data.building_ledger_outline_summaries (
mgm_bldrgst_pk text NULL,
regstr_gb_cd text NULL,
regstr_gb_cd_nm text NULL,
regstr_kind_cd text NULL,
regstr_kind_cd_nm text NULL,
new_regstr_gb_cd text NULL,
new_regstr_gb_cd_nm text NULL,
plat_plc text NULL,
new_plat_plc text NULL,
bld_nm text NULL,
sigungu_cd text NULL,
bjdong_cd text NULL,
plat_gb_cd text NULL,
bun text NULL,
ji text NULL,
splot_nm text NULL,
block text NULL,
lot text NULL,
bylot_cnt text NULL,
na_road_cd text NULL,
na_bjdong_cd text NULL,
na_ugrnd_cd text NULL,
na_main_bun text NULL,
na_sub_bun text NULL,
plat_area text NULL,
arch_area text NULL,
bc_rat text NULL,
tot_area text NULL,
vl_rat_estm_tot_area text NULL,
vl_rat text NULL,
main_purps_cd text NULL,
main_purps_cd_nm text NULL,
etc_purps text NULL,
hhld_cnt text NULL,
fmly_cnt text NULL,
main_bld_cnt text NULL,
atch_bld_cnt text NULL,
atch_bld_area text NULL,
tot_pkng_cnt text NULL,
indr_mech_ut_cnt text NULL,
indr_mech_area text NULL,
oudr_mech_ut_cnt text NULL,
oudr_mech_area text NULL,
indr_auto_ut_cnt text NULL,
indr_auto_area text NULL,
oudr_auto_ut_cnt text NULL,
oudr_auto_area text NULL,
pms_day text NULL,
stcns_day text NULL,
use_apr_day text NULL,
pmsno_year text NULL,
pmsno_kik_cd text NULL,
pmsno_kik_cd_nm text NULL,
pmsno_gb_cd text NULL,
pmsno_gb_cd_nm text NULL,
ho_cnt text NULL,
engr_grade text NULL,
engr_rat text NULL,
engr_epi text NULL,
gn_bld_grade text NULL,
gn_bld_cert text NULL,
itg_bld_grade text NULL,
itg_bld_cert text NULL,
crtn_day text NULL,
create_dt timestamptz NULL
);
CREATE INDEX building_ledger_outline_summaries_custom_pnu_index ON external_data.building_ledger_outline_summaries USING btree (sigungu_cd, bjdong_cd, plat_gb_cd, bun, ji);
CREATE INDEX building_ledger_outline_summaries_mgm_bldrgst_pk_index ON external_data.building_ledger_outline_summaries USING btree (mgm_bldrgst_pk);

CREATE TABLE external_data.r3_real_estate_trade (
id bigserial NOT NULL,
jibun_address text NOT NULL,
road_address text NULL,
pnu text NOT NULL,
bjd_code text NOT NULL,
building_name text NULL,
property text NOT NULL,
is_multi_unit bool NULL,
rent_type text NOT NULL,
contract_date date NOT NULL,
contract_year varchar(4) NOT NULL,
contract_ym varchar(7) NOT NULL,
effective_amount int8 NOT NULL,
monthly_rent_amount int8 NULL,
building_amount_per_nla_m2 numeric(20, 2) NULL,
building_amount_per_gla_m2 numeric(20, 2) NULL,
land_amount_per_m2 numeric(20, 2) NULL,
nla_m2 numeric(20, 2) NULL,
gla_m2 numeric(20, 2) NULL,
land_m2 numeric(20, 2) NULL,
pla_m2 numeric(20, 2) NULL,
land_use_right_m2 numeric(20, 2) NULL,
is_shared bool DEFAULT false NOT NULL,
construction_year int4 NULL,
floor text NULL,
buyer text NULL,
seller text NULL,
contract_type text NULL,
building_structure text NULL,
land_use text NULL,
contract_status text NULL,
contract_period text NULL,
registration_date date NULL,
unit text NULL,
contract_area numeric(20, 2) NULL,
created_at timestamp DEFAULT now() NOT NULL,
created_by int4 DEFAULT 999999998 NOT NULL,
updated_at timestamp NULL,
updated_by int4 NULL,
dwelling_type text NULL,
zonning text NULL,
bld_use_or_land_category text NULL,
CONSTRAINT r3_real_estate_trade_check_is_shared CHECK (((is_shared = false) OR (property = ANY (ARRAY['LAND'::text, 'COMMERCIAL_BUILDING'::text, 'SHOPPING_AND_OFFICE'::text, 'FACTORY_WAREHOUSE'::text, 'FACTORY_WAREHOUSE_MULTI'::text])))),
CONSTRAINT r3_real_estate_trade_pkey PRIMARY KEY (id)
);


-- external_data.boundary_region definition

-- Drop table

-- DROP TABLE external_data.boundary_region;

CREATE TABLE external_data.boundary_region (
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
CREATE INDEX idx_boundary_region_geom ON external_data.boundary_region USING gist (geom);
CREATE INDEX idx_boundary_region_region_code_gubun ON external_data.boundary_region USING btree (region_code, gubun);
CREATE INDEX idx_boundary_region_sido ON external_data.boundary_region USING btree ("left"(region_code, 2));
CREATE INDEX idx_boundary_region_sigungu ON external_data.boundary_region USING btree ("left"(region_code, 5));