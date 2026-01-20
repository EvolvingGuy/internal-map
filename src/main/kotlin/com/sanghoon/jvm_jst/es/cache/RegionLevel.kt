package com.sanghoon.jvm_jst.es.cache

enum class RegionLevel {
    SIDO, SIGUNGU, DONG;

    companion object {
        fun from(gubun: String): RegionLevel = when (gubun) {
            "sido" -> SIDO
            "sigungu" -> SIGUNGU
            "dong", "li" -> DONG
            else -> throw IllegalArgumentException("Unknown gubun: $gubun")
        }
    }
}
