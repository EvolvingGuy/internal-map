```sql
CREATE TABLE external_data.land_characteristic (
    pnu           TEXT NULL,
    bjdong_cd     TEXT NULL,
    bjdong_nm     TEXT NULL,
    regstr_gb_cd  TEXT NULL,
    regstr_gb     TEXT NULL,
    jibun         TEXT NULL,
    jimok_sign    TEXT NULL,
    std_year      TEXT NULL,
    std_month     TEXT NULL,
    jimok_cd      TEXT NULL,
    jimok         TEXT NULL,
    area          TEXT NULL,
    jiyuk_cd_1    TEXT NULL,
    jiyuk_1       TEXT NULL,
    jiyuk_cd_2    TEXT NULL,
    jiyuk_2       TEXT NULL,
    land_use_cd   TEXT NULL,
    land_use      TEXT NULL,
    height_cd     TEXT NULL,
    height        TEXT NULL,
    shape_cd      TEXT NULL,
    shape         TEXT NULL,
    road_cd       TEXT NULL,
    road          TEXT NULL,
    price         TEXT NULL,
    crtn_day      TEXT NULL,
    geometry      PUBLIC.GEOMETRY(GEOMETRY, 4326) NULL,
    create_dt     TIMESTAMPTZ NULL,
    center        PUBLIC.GEOMETRY(POINT, 4326) NULL,
    is_donut      BOOL NULL
);

CREATE INDEX idx_lc_bjdong_cd_left2 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 2));
CREATE INDEX idx_lc_bjdong_cd_left5 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 5));
CREATE INDEX idx_lc_bjdong_cd_left8 ON external_data.land_characteristic USING btree ("left"(bjdong_cd, 8));
CREATE INDEX land_characteristic_bjdong_cd_index ON external_data.land_characteristic USING btree (bjdong_cd);
CREATE INDEX land_characteristic_pnu_index ON external_data.land_characteristic USING btree (pnu);
```
