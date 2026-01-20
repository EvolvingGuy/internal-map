package com.sanghoon.jvm_jst.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "building_ledger_outline", schema = "external_data")
data class BuildingLedgerOutline(
    @Id
    @Column(name = "mgm_bldrgst_pk")
    val mgmBldrgstPk: String,

    @Column(name = "regstr_gb_cd")
    val regstrGbCd: String? = null,

    @Column(name = "regstr_gb_cd_nm")
    val regstrGbCdNm: String? = null,

    @Column(name = "regstr_kind_cd")
    val regstrKindCd: String? = null,

    @Column(name = "regstr_kind_cd_nm")
    val regstrKindCdNm: String? = null,

    @Column(name = "plat_plc")
    val platPlc: String? = null,

    @Column(name = "new_plat_plc")
    val newPlatPlc: String? = null,

    @Column(name = "bld_nm")
    val bldNm: String? = null,

    @Column(name = "sigungu_cd")
    val sigunguCd: String? = null,

    @Column(name = "bjdong_cd")
    val bjdongCd: String? = null,

    @Column(name = "plat_gb_cd")
    val platGbCd: String? = null,

    @Column(name = "bun")
    val bun: String? = null,

    @Column(name = "ji")
    val ji: String? = null,

    @Column(name = "splot_nm")
    val splotNm: String? = null,

    @Column(name = "block")
    val block: String? = null,

    @Column(name = "lot")
    val lot: String? = null,

    @Column(name = "bylot_cnt")
    val bylotCnt: String? = null,

    @Column(name = "na_road_cd")
    val naRoadCd: String? = null,

    @Column(name = "na_bjdong_cd")
    val naBjdongCd: String? = null,

    @Column(name = "na_ugrnd_cd")
    val naUgrndCd: String? = null,

    @Column(name = "na_main_bun")
    val naMainBun: String? = null,

    @Column(name = "na_sub_bun")
    val naSubBun: String? = null,

    @Column(name = "dong_nm")
    val dongNm: String? = null,

    @Column(name = "main_atch_gb_cd")
    val mainAtchGbCd: String? = null,

    @Column(name = "main_atch_gb_cd_nm")
    val mainAtchGbCdNm: String? = null,

    @Column(name = "plat_area")
    val platArea: String? = null,

    @Column(name = "arch_area")
    val archArea: String? = null,

    @Column(name = "bc_rat")
    val bcRat: String? = null,

    @Column(name = "tot_area")
    val totArea: String? = null,

    @Column(name = "vl_rat_estm_tot_area")
    val vlRatEstmTotArea: String? = null,

    @Column(name = "vl_rat")
    val vlRat: String? = null,

    @Column(name = "strct_cd")
    val strctCd: String? = null,

    @Column(name = "strct_cd_nm")
    val strctCdNm: String? = null,

    @Column(name = "etc_strct")
    val etcStrct: String? = null,

    @Column(name = "main_purps_cd")
    val mainPurpsCd: String? = null,

    @Column(name = "main_purps_cd_nm")
    val mainPurpsCdNm: String? = null,

    @Column(name = "etc_purps")
    val etcPurps: String? = null,

    @Column(name = "roof_cd")
    val roofCd: String? = null,

    @Column(name = "roof_cd_nm")
    val roofCdNm: String? = null,

    @Column(name = "etc_roof")
    val etcRoof: String? = null,

    @Column(name = "hhld_cnt")
    val hhldCnt: String? = null,

    @Column(name = "fmly_cnt")
    val fmlyCnt: String? = null,

    @Column(name = "heit")
    val heit: String? = null,

    @Column(name = "grnd_flr_cnt")
    val grndFlrCnt: String? = null,

    @Column(name = "ugrnd_flr_cnt")
    val ugrndFlrCnt: String? = null,

    @Column(name = "ride_use_elvt_cnt")
    val rideUseElvtCnt: String? = null,

    @Column(name = "emgen_use_elvt_cnt")
    val emgenUseElvtCnt: String? = null,

    @Column(name = "atch_bld_cnt")
    val atchBldCnt: String? = null,

    @Column(name = "atch_bld_area")
    val atchBldArea: String? = null,

    @Column(name = "tot_dong_tot_area")
    val totDongTotArea: String? = null,

    @Column(name = "indr_mech_ut_cnt")
    val indrMechUtCnt: String? = null,

    @Column(name = "indr_mech_area")
    val indrMechArea: String? = null,

    @Column(name = "oudr_mech_ut_cnt")
    val oudrMechUtCnt: String? = null,

    @Column(name = "oudr_mech_area")
    val oudrMechArea: String? = null,

    @Column(name = "indr_auto_ut_cnt")
    val indrAutoUtCnt: String? = null,

    @Column(name = "indr_auto_area")
    val indrAutoArea: String? = null,

    @Column(name = "oudr_auto_ut_cnt")
    val oudrAutoUtCnt: String? = null,

    @Column(name = "oudr_auto_area")
    val oudrAutoArea: String? = null,

    @Column(name = "pms_day")
    val pmsDay: String? = null,

    @Column(name = "stcns_day")
    val stcnsDay: String? = null,

    @Column(name = "use_apr_day")
    val useAprDay: String? = null,

    @Column(name = "pmsno_year")
    val pmsnoYear: String? = null,

    @Column(name = "pmsno_kik_cd")
    val pmsnoKikCd: String? = null,

    @Column(name = "pmsno_kik_cd_nm")
    val pmsnoKikCdNm: String? = null,

    @Column(name = "pmsno_gb_cd")
    val pmsnoGbCd: String? = null,

    @Column(name = "pmsno_gb_cd_nm")
    val pmsnoGbCdNm: String? = null,

    @Column(name = "ho_cnt")
    val hoCnt: String? = null,

    @Column(name = "engr_grade")
    val engrGrade: String? = null,

    @Column(name = "engr_rat")
    val engrRat: String? = null,

    @Column(name = "engr_epi")
    val engrEpi: String? = null,

    @Column(name = "gn_bld_grade")
    val gnBldGrade: String? = null,

    @Column(name = "gn_bld_cert")
    val gnBldCert: String? = null,

    @Column(name = "itg_bld_grade")
    val itgBldGrade: String? = null,

    @Column(name = "itg_bld_cert")
    val itgBldCert: String? = null,

    @Column(name = "crtn_day")
    val crtnDay: String? = null,

    @Column(name = "rserthqk_dsgn_apply_yn")
    val rserthqkDsgnApplyYn: String? = null,

    @Column(name = "rserthqk_ablty")
    val rserthqkAblty: String? = null,

    @Column(name = "create_dt")
    val createDt: LocalDateTime? = null
)
