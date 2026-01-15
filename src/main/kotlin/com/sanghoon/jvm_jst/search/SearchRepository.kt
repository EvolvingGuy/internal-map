package com.sanghoon.jvm_jst.search

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SearchRepository : JpaRepository<SearchEntity, String> {
    @Query(
        value = """
        SELECT pnu, bjdong_cd, jibun_address, road_address, building_name, lat, lng
        FROM external_data.integrated_search_index
        WHERE search_text LIKE '%' || replace(:keyword, ' ', '') || '%'
        LIMIT 50
    """,
        nativeQuery = true
    )
    fun searchByKeyword(keyword: String): List<Array<Any?>>
}
