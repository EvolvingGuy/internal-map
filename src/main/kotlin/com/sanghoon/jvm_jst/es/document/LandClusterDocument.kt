package com.sanghoon.jvm_jst.es.document

import com.sanghoon.jvm_jst.entity.LandCharacteristic

/**
 * land_cluster ES 인덱스 문서
 */
data class LandClusterDocument(
    val pnu: String,
    val sido: String,
    val jimokCd: String? = null,
    val jiyukCd1: String? = null,
    val mainPurpsCd: String? = null,
    val location: Map<String, Double>? = null
) {
    companion object {
        fun from(entity: LandCharacteristic): LandClusterDocument {
            val center = entity.center
            return LandClusterDocument(
                pnu = entity.pnu,
                sido = entity.pnu.take(2),
                jimokCd = entity.jimokCd,
                jiyukCd1 = entity.jiyukCd1,
                mainPurpsCd = null,
                location = center?.let { mapOf("lat" to it.y, "lon" to it.x) }
            )
        }
    }
}
