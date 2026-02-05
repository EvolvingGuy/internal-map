-- manage.geo_poc_land Migration SQL
-- 시도(SD) 단위 파라미터(:sd_code)로 분할 실행 (17회)
-- SD: 11,26,27,28,29,30,31,36,41,43,44,46,47,48,50,51,52
-- 실행: psql -v sd_code="'11'" -f geo_poc_land_migration.sql
INSERT INTO manage.geo_poc_land (
    pnu, sd, sgg, emd,
    jiyuk_cd_1, jimok_cd, area, price,
    center_lat, center_lng,
    buildings, trades
)
WITH
land_base AS (
    SELECT
        lc.pnu,
        LEFT(lc.pnu, 2)                                          AS sd,
        LEFT(lc.pnu, 5)                                          AS sgg,
        LEFT(lc.pnu, 8)                                          AS emd,
        lc.jiyuk_cd_1,
        lc.jimok_cd,
        NULLIF(lc.area, '')::DOUBLE PRECISION                     AS area,
        NULLIF(lc.price, '')::NUMERIC::BIGINT                     AS price,
        ST_Y(lc.center)                                           AS center_lat,
        ST_X(lc.center)                                           AS center_lng,
        LEFT(lc.pnu, 10)
            || CASE SUBSTRING(lc.pnu FROM 11 FOR 1)
                   WHEN '1' THEN '0'
                   ELSE '1'
               END
            || SUBSTRING(lc.pnu FROM 12)                          AS building_pnu
    FROM external_data.land_characteristic lc
    WHERE LEFT(lc.pnu, 2) = :sd_code::TEXT
),
bldg_summaries AS (
    SELECT
        s.pnu                    AS building_pnu,
        jsonb_build_object(
            'mgmBldrgstPk',   s.mgm_bldrgst_pk,
            'mainPurpsCdNm',  s.main_purps_cd_nm,
            'regstrGbCdNm',   s.regstr_gb_cd_nm,
            'pmsDay',         s.pms_day,
            'stcnsDay',       s.stcns_day,
            'useAprDay',      s.use_apr_day,
            'totArea',        NULLIF(s.tot_area, '')::NUMERIC,
            'platArea',       NULLIF(s.plat_area, '')::NUMERIC,
            'archArea',       NULLIF(s.arch_area, '')::NUMERIC
        )                        AS bldg_json
    FROM external_data.building_ledger_outline_summaries s
    WHERE s.pnu IN (SELECT DISTINCT building_pnu FROM land_base)
      AND s.mgm_bldrgst_pk IS NOT NULL
      AND s.mgm_bldrgst_pk <> ''
),
bldg_outlines AS (
    SELECT
        o.pnu                    AS building_pnu,
        jsonb_build_object(
            'mgmBldrgstPk',   o.mgm_bldrgst_pk,
            'mainPurpsCdNm',  o.main_purps_cd_nm,
            'regstrGbCdNm',   o.regstr_gb_cd_nm,
            'pmsDay',         o.pms_day,
            'stcnsDay',       o.stcns_day,
            'useAprDay',      o.use_apr_day,
            'totArea',        NULLIF(o.tot_area, '')::NUMERIC,
            'platArea',       NULLIF(o.plat_area, '')::NUMERIC,
            'archArea',       NULLIF(o.arch_area, '')::NUMERIC
        )                        AS bldg_json
    FROM external_data.building_ledger_outline o
    WHERE o.pnu IN (SELECT DISTINCT building_pnu FROM land_base)
      AND o.mgm_bldrgst_pk IS NOT NULL
      AND o.mgm_bldrgst_pk <> ''
      AND o.pnu NOT IN (SELECT DISTINCT building_pnu FROM bldg_summaries)
),
bldg_all AS (
    SELECT
        building_pnu,
        jsonb_agg(bldg_json) AS buildings
    FROM (
        SELECT building_pnu, bldg_json FROM bldg_summaries
        UNION ALL
        SELECT building_pnu, bldg_json FROM bldg_outlines
    ) combined
    GROUP BY building_pnu
),
trade_agg AS (
    SELECT
        t.pnu,
        jsonb_agg(
            jsonb_build_object(
                'property',              t.property,
                'contractDate',          t.contract_date,
                'effectiveAmount',       t.effective_amount,
                'buildingAmountPerM2',   t.building_amount_per_nla_m2,
                'landAmountPerM2',       t.land_amount_per_m2
            )
        ) AS trades
    FROM external_data.r3_real_estate_trade t
    WHERE t.pnu IN (SELECT DISTINCT pnu FROM land_base)
    GROUP BY t.pnu
)
SELECT
    lb.pnu,
    lb.sd,
    lb.sgg,
    lb.emd,
    lb.jiyuk_cd_1,
    lb.jimok_cd,
    lb.area,
    lb.price,
    lb.center_lat,
    lb.center_lng,
    ba.buildings,
    ta.trades
FROM land_base lb
LEFT JOIN bldg_all ba ON ba.building_pnu = lb.building_pnu
LEFT JOIN trade_agg ta ON ta.pnu = lb.pnu
ON CONFLICT (pnu) DO UPDATE SET
    sd           = EXCLUDED.sd,
    sgg          = EXCLUDED.sgg,
    emd          = EXCLUDED.emd,
    jiyuk_cd_1   = EXCLUDED.jiyuk_cd_1,
    jimok_cd     = EXCLUDED.jimok_cd,
    area         = EXCLUDED.area,
    price        = EXCLUDED.price,
    center_lat   = EXCLUDED.center_lat,
    center_lng   = EXCLUDED.center_lng,
    buildings    = EXCLUDED.buildings,
    trades       = EXCLUDED.trades;
