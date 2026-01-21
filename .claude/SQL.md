create table land_characteristic
(
pnu          text,
bjdong_cd    text,
bjdong_nm    text,
regstr_gb_cd text,
regstr_gb    text,
jibun        text,
jimok_sign   text,
std_year     text,
std_month    text,
jimok_cd     text,
jimok        text,
area         text,
jiyuk_cd_1   text,
jiyuk_1      text,
jiyuk_cd_2   text,
jiyuk_2      text,
land_use_cd  text,
land_use     text,
height_cd    text,
height       text,
shape_cd     text,
shape        text,
road_cd      text,
road         text,
price        text,
crtn_day     text,
geometry     geometry(Geometry, 4326),
create_dt    timestamp with time zone,
center       geometry(Point, 4326),
is_donut     boolean
);

alter table land_characteristic
owner to sanghoonseo;

create index land_characteristic_pnu_index
on land_characteristic (pnu);

create index land_characteristic_bjdong_cd_index
on land_characteristic (bjdong_cd);

create index idx_lc_bjdong_cd_left8
on land_characteristic ("left"(bjdong_cd, 8));

create index idx_lc_bjdong_cd_left2
on land_characteristic ("left"(bjdong_cd, 2));

create index idx_lc_bjdong_cd_left5
on land_characteristic ("left"(bjdong_cd, 5));

-- auto-generated definition
create table building_ledger_outline
(
mgm_bldrgst_pk         text,
regstr_gb_cd           text,
regstr_gb_cd_nm        text,
regstr_kind_cd         text,
regstr_kind_cd_nm      text,
plat_plc               text,
new_plat_plc           text,
bld_nm                 text,
sigungu_cd             text,
bjdong_cd              text,
plat_gb_cd             text,
bun                    text,
ji                     text,
splot_nm               text,
block                  text,
lot                    text,
bylot_cnt              text,
na_road_cd             text,
na_bjdong_cd           text,
na_ugrnd_cd            text,
na_main_bun            text,
na_sub_bun             text,
dong_nm                text,
main_atch_gb_cd        text,
main_atch_gb_cd_nm     text,
plat_area              text,
arch_area              text,
bc_rat                 text,
tot_area               text,
vl_rat_estm_tot_area   text,
vl_rat                 text,
strct_cd               text,
strct_cd_nm            text,
etc_strct              text,
main_purps_cd          text,
main_purps_cd_nm       text,
etc_purps              text,
roof_cd                text,
roof_cd_nm             text,
etc_roof               text,
hhld_cnt               text,
fmly_cnt               text,
heit                   text,
grnd_flr_cnt           text,
ugrnd_flr_cnt          text,
ride_use_elvt_cnt      text,
emgen_use_elvt_cnt     text,
atch_bld_cnt           text,
atch_bld_area          text,
tot_dong_tot_area      text,
indr_mech_ut_cnt       text,
indr_mech_area         text,
oudr_mech_ut_cnt       text,
oudr_mech_area         text,
indr_auto_ut_cnt       text,
indr_auto_area         text,
oudr_auto_ut_cnt       text,
oudr_auto_area         text,
pms_day                text,
stcns_day              text,
use_apr_day            text,
pmsno_year             text,
pmsno_kik_cd           text,
pmsno_kik_cd_nm        text,
pmsno_gb_cd            text,
pmsno_gb_cd_nm         text,
ho_cnt                 text,
engr_grade             text,
engr_rat               text,
engr_epi               text,
gn_bld_grade           text,
gn_bld_cert            text,
itg_bld_grade          text,
itg_bld_cert           text,
crtn_day               text,
rserthqk_dsgn_apply_yn text,
rserthqk_ablty         text,
create_dt              timestamp with time zone
);

alter table building_ledger_outline
owner to sanghoonseo;

-- auto-generated definition
create table building_ledger_outline_summaries
(
mgm_bldrgst_pk       text,
regstr_gb_cd         text,
regstr_gb_cd_nm      text,
regstr_kind_cd       text,
regstr_kind_cd_nm    text,
new_regstr_gb_cd     text,
new_regstr_gb_cd_nm  text,
plat_plc             text,
new_plat_plc         text,
bld_nm               text,
sigungu_cd           text,
bjdong_cd            text,
plat_gb_cd           text,
bun                  text,
ji                   text,
splot_nm             text,
block                text,
lot                  text,
bylot_cnt            text,
na_road_cd           text,
na_bjdong_cd         text,
na_ugrnd_cd          text,
na_main_bun          text,
na_sub_bun           text,
plat_area            text,
arch_area            text,
bc_rat               text,
tot_area             text,
vl_rat_estm_tot_area text,
vl_rat               text,
main_purps_cd        text,
main_purps_cd_nm     text,
etc_purps            text,
hhld_cnt             text,
fmly_cnt             text,
main_bld_cnt         text,
atch_bld_cnt         text,
atch_bld_area        text,
tot_pkng_cnt         text,
indr_mech_ut_cnt     text,
indr_mech_area       text,
oudr_mech_ut_cnt     text,
oudr_mech_area       text,
indr_auto_ut_cnt     text,
indr_auto_area       text,
oudr_auto_ut_cnt     text,
oudr_auto_area       text,
pms_day              text,
stcns_day            text,
use_apr_day          text,
pmsno_year           text,
pmsno_kik_cd         text,
pmsno_kik_cd_nm      text,
pmsno_gb_cd          text,
pmsno_gb_cd_nm       text,
ho_cnt               text,
engr_grade           text,
engr_rat             text,
engr_epi             text,
gn_bld_grade         text,
gn_bld_cert          text,
itg_bld_grade        text,
itg_bld_cert         text,
crtn_day             text,
create_dt            timestamp with time zone
);

alter table building_ledger_outline_summaries
owner to sanghoonseo;

create index building_ledger_outline_summaries_custom_pnu_index
on building_ledger_outline_summaries (sigungu_cd, bjdong_cd, plat_gb_cd, bun, ji);

create index building_ledger_outline_summaries_mgm_bldrgst_pk_index
on building_ledger_outline_summaries (mgm_bldrgst_pk);

-- auto-generated definition
create table r3_real_estate_trade
(
id                         bigserial
primary key,
jibun_address              text                        not null,
road_address               text,
pnu                        text                        not null,
bjd_code                   text                        not null,
building_name              text,
property                   text                        not null,
is_multi_unit              boolean,
rent_type                  text                        not null,
contract_date              date                        not null,
contract_year              varchar(4)                  not null,
contract_ym                varchar(7)                  not null,
effective_amount           bigint                      not null,
monthly_rent_amount        bigint,
building_amount_per_nla_m2 numeric(20, 2),
building_amount_per_gla_m2 numeric(20, 2),
land_amount_per_m2         numeric(20, 2),
nla_m2                     numeric(20, 2),
gla_m2                     numeric(20, 2),
land_m2                    numeric(20, 2),
pla_m2                     numeric(20, 2),
land_use_right_m2          numeric(20, 2),
is_shared                  boolean   default false     not null,
construction_year          integer,
floor                      text,
buyer                      text,
seller                     text,
contract_type              text,
building_structure         text,
land_use                   text,
contract_status            text,
contract_period            text,
registration_date          date,
unit                       text,
contract_area              numeric(20, 2),
created_at                 timestamp default now()     not null,
created_by                 integer   default 999999998 not null,
updated_at                 timestamp,
updated_by                 integer,
dwelling_type              text,
zonning                    text,
bld_use_or_land_category   text,
constraint r3_real_estate_trade_check_is_shared
check ((is_shared = false) OR (property = ANY
(ARRAY ['LAND'::text, 'COMMERCIAL_BUILDING'::text, 'SHOPPING_AND_OFFICE'::text, 'FACTORY_WAREHOUSE'::text, 'FACTORY_WAREHOUSE_MULTI'::text])))
);

alter table r3_real_estate_trade
owner to sanghoonseo;


CREATE INDEX idx_r3_ret_pnu ON external_data.r3_real_estate_trade (pnu);