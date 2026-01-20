package com.sanghoon.jvm_jst.es.util

import com.sanghoon.jvm_jst.entity.BuildingLedgerOutline
import com.sanghoon.jvm_jst.entity.BuildingLedgerOutlineSummaries

/**
 * PNU 관련 유틸리티
 */
object PnuUtils {

    /**
     * PNU에서 시도 코드 추출 (앞 2자리)
     */
    fun extractSd(pnu: String): String {
        require(pnu.length >= 2) { "PNU must be at least 2 characters" }
        return pnu.substring(0, 2)
    }

    /**
     * PNU에서 시군구 코드 추출 (앞 5자리)
     */
    fun extractSgg(pnu: String): String {
        require(pnu.length >= 5) { "PNU must be at least 5 characters" }
        return pnu.substring(0, 5)
    }

    /**
     * PNU에서 읍면동 코드 추출 (앞 10자리)
     */
    fun extractEmd(pnu: String): String {
        require(pnu.length >= 10) { "PNU must be at least 10 characters" }
        return pnu.substring(0, 10)
    }

    /**
     * Land PNU -> Building PNU 변환
     *
     * 11번째 자리 (platGbCd) 변환 규칙:
     * - Land '1' (일반) -> Building '0'
     * - Land '2' (산) -> Building '1'
     * - 기타 -> Building '1'
     */
    fun convertLandPnuToBuilding(landPnu: String): String {
        if (landPnu.length < 11) return landPnu

        val platGbCd = landPnu[10]
        val buildingPlatGbCd = if (platGbCd == '1') '0' else '1'

        return landPnu.substring(0, 10) + buildingPlatGbCd + landPnu.substring(11)
    }

    /**
     * BuildingLedgerOutlineSummaries에서 PNU 조합
     */
    fun buildPnuFrom(entity: BuildingLedgerOutlineSummaries): String {
        return buildString {
            append(entity.sigunguCd ?: "")
            append(entity.bjdongCd ?: "")
            append(entity.platGbCd ?: "")
            append(entity.bun ?: "")
            append(entity.ji ?: "")
        }
    }

    /**
     * BuildingLedgerOutline에서 PNU 조합
     */
    fun buildPnuFrom(entity: BuildingLedgerOutline): String {
        return buildString {
            append(entity.sigunguCd ?: "")
            append(entity.bjdongCd ?: "")
            append(entity.platGbCd ?: "")
            append(entity.bun ?: "")
            append(entity.ji ?: "")
        }
    }
}
