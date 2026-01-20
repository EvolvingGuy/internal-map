package com.sanghoon.jvm_jst.es.repository

import com.sanghoon.jvm_jst.es.document.LandAggregationDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface LandAggregationRepository : ElasticsearchRepository<LandAggregationDocument, String> {

    /**
     * 법정동코드로 조회
     */
    fun findByBjdongCd(bjdongCd: String): List<LandAggregationDocument>

    /**
     * 법정동코드 목록으로 조회
     */
    fun findByBjdongCdIn(bjdongCds: Collection<String>): List<LandAggregationDocument>

    /**
     * 시도코드로 조회
     */
    fun findBySidoCd(sidoCd: String): List<LandAggregationDocument>

    /**
     * 시군구코드로 조회
     */
    fun findBySggCd(sggCd: String): List<LandAggregationDocument>

    /**
     * 읍면동코드로 조회
     */
    fun findByEmdCd(emdCd: String): List<LandAggregationDocument>
}
