-- =============================================================
-- manage.geo_poc_land DDL
-- ES LBT 문서와 1:1 매핑되는 단일 비정규화 테이블
-- 인덱싱 시 이 테이블만 읽으면 ES 문서를 바로 구성 가능
-- =============================================================

CREATE SCHEMA IF NOT EXISTS manage;

CREATE TABLE manage.geo_poc_land (
    pnu          VARCHAR(19)      NOT NULL,
    sd           VARCHAR(2)       NOT NULL,
    sgg          VARCHAR(5)       NOT NULL,
    emd          VARCHAR(8)       NOT NULL,
    jiyuk_cd_1   VARCHAR(20)      NULL,
    jimok_cd     VARCHAR(20)      NULL,
    area         DOUBLE PRECISION NULL,
    price        BIGINT           NULL,
    center_lat   DOUBLE PRECISION NULL,
    center_lng   DOUBLE PRECISION NULL,
    buildings    JSONB            NULL,
    trades       JSONB            NULL,
    CONSTRAINT geo_poc_land_pkey PRIMARY KEY (pnu)
);

-- 행정구역 코드 인덱스 (SD / SGG / EMD 필터용)
CREATE INDEX idx_gpl_sd  ON manage.geo_poc_land USING btree (sd);
CREATE INDEX idx_gpl_sgg ON manage.geo_poc_land USING btree (sgg);
CREATE INDEX idx_gpl_emd ON manage.geo_poc_land USING btree (emd);
