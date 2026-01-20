```sql
CREATE TABLE external_data.r3_real_estate_trade
(
    id                         bigserial                   NOT NULL,
    jibun_address              text                        NOT NULL,
    road_address               text                        NULL,
    pnu                        text                        NOT NULL,
    bjd_code                   text                        NOT NULL,
    building_name              text                        NULL,
    property                   text                        NOT NULL,
    is_multi_unit              bool                        NULL,
    rent_type                  text                        NOT NULL,
    contract_date              date                        NOT NULL,
    contract_year              varchar(4)                  NOT NULL,
    contract_ym                varchar(7)                  NOT NULL,
    effective_amount           int8                        NOT NULL,
    monthly_rent_amount        int8                        NULL,
    building_amount_per_nla_m2 numeric(20, 2)              NULL,
    building_amount_per_gla_m2 numeric(20, 2)              NULL,
    land_amount_per_m2         numeric(20, 2)              NULL,
    nla_m2                     numeric(20, 2)              NULL,
    gla_m2                     numeric(20, 2)              NULL,
    land_m2                    numeric(20, 2)              NULL,
    pla_m2                     numeric(20, 2)              NULL,
    land_use_right_m2          numeric(20, 2)              NULL,
    is_shared                  bool      DEFAULT false     NOT NULL,
    construction_year          int4                        NULL,
    floor                      text                        NULL,
    buyer                      text                        NULL,
    seller                     text                        NULL,
    contract_type              text                        NULL,
    building_structure         text                        NULL,
    land_use                   text                        NULL,
    contract_status            text                        NULL,
    contract_period            text                        NULL,
    registration_date          date                        NULL,
    unit                       text                        NULL,
    contract_area              numeric(20, 2)              NULL,
    created_at                 timestamp DEFAULT now()     NOT NULL,
    created_by                 int4      DEFAULT 999999998 NOT NULL,
    updated_at                 timestamp                   NULL,
    updated_by                 int4                        NULL,
    dwelling_type              text                        NULL,
    zonning                    text                        NULL,
    bld_use_or_land_category   text                        NULL,
    CONSTRAINT r3_real_estate_trade_check_is_shared CHECK (((is_shared = false) OR (property = ANY
                                                                                    (ARRAY ['LAND'::text, 'COMMERCIAL_BUILDING'::text, 'SHOPPING_AND_OFFICE'::text, 'FACTORY_WAREHOUSE'::text, 'FACTORY_WAREHOUSE_MULTI'::text])))),
    CONSTRAINT r3_real_estate_trade_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_r3_ret_pnu ON external_data.r3_real_estate_trade USING btree (pnu);
CREATE INDEX idx_r3_ret_pnu_id_desc ON external_data.r3_real_estate_trade USING btree (pnu, id DESC);
CREATE INDEX r3_real_estate_trade_pnu_index ON external_data.r3_real_estate_trade USING btree (pnu);
);
```