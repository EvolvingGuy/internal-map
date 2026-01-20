package com.datahub.geo_poc.util

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 파싱 유틸리티
 */
object ParsingUtils {

    /**
     * 문자열 -> Double 변환 (빈 값은 null)
     */
    fun toDoubleOrNull(value: String?): Double? {
        return value?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    }

    /**
     * 문자열 -> Long 변환 (빈 값은 null)
     * 소수점 문자열도 처리 (예: "4190.0" -> 4190)
     */
    fun toLongOrNull(value: String?): Long? {
        val str = value?.takeIf { it.isNotBlank() } ?: return null
        return str.toLongOrNull() ?: str.toDoubleOrNull()?.toLong()
    }

    /**
     * 문자열 -> BigDecimal 변환 (빈 값은 null)
     */
    fun toBigDecimalOrNull(value: String?): BigDecimal? {
        return value?.takeIf { it.isNotBlank() }?.toBigDecimalOrNull()
    }

    /**
     * 날짜 문자열 -> LocalDate 변환
     *
     * 지원 형식:
     * - yyyyMMdd: 정상 파싱
     * - yyyyMM: 해당 월 1일로 변환
     * - yyyy: 해당 연도 1월 1일로 변환
     *
     * 예외 처리:
     * - 불가능한 날짜 (13월, 32일 등): null
     * - 포맷 오류: null
     * - 빈 값: null
     */
    fun toLocalDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null

        val cleaned = value.trim().filter { it.isDigit() }
        if (cleaned.isEmpty()) return null

        return try {
            when (cleaned.length) {
                8 -> parseYyyyMmDd(cleaned)
                6 -> parseYyyyMm(cleaned)
                4 -> parseYyyy(cleaned)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseYyyyMmDd(value: String): LocalDate? {
        val year = value.substring(0, 4).toInt()
        val month = value.substring(4, 6).toInt()
        val day = value.substring(6, 8).toInt()

        if (!isValidMonth(month)) return null
        if (!isValidDay(day)) return null

        return LocalDate.of(year, month, day)
    }

    private fun parseYyyyMm(value: String): LocalDate? {
        val year = value.substring(0, 4).toInt()
        val month = value.substring(4, 6).toInt()

        if (!isValidMonth(month)) return null

        return LocalDate.of(year, month, 1)
    }

    private fun parseYyyy(value: String): LocalDate {
        val year = value.toInt()
        return LocalDate.of(year, 1, 1)
    }

    private fun isValidMonth(month: Int): Boolean = month in 1..12
    private fun isValidDay(day: Int): Boolean = day in 1..31
}
