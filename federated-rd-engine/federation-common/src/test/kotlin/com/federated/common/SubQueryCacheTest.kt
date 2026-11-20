package com.federated.common

import com.federated.common.cache.SubQueryCache
import com.federated.common.rmi.FederatedQueryResponse
import com.federated.common.rmi.HospitalResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubQueryCacheTest {

    @Test
    fun `test cache store, retrieve, normalization, and stats`() {
        val cache = SubQueryCache(ttlSeconds = 60, maxSize = 100)
        val query1 = "SELECT * WHERE { ?s ?p ?o }"
        val query1Normalized = "  SELECT   * WHERE  {  ?s ?p ?o  }  "

        val response = FederatedQueryResponse(
            queryId = "q1",
            respondingNodeId = "hospital-b",
            success = true,
            executionTimeMs = 12L,
            results = listOf(
                HospitalResult(condition = "COVID-19", symptom = "Fever", hospitalId = "hospital-b")
            )
        )

        cache.put("hospital-b", query1, response)

        val retrieved = cache.get("hospital-b", query1Normalized)
        assertNotNull(retrieved)
        assertEquals("hospital-b", retrieved?.respondingNodeId)
        assertEquals(1, retrieved?.results?.size)

        val stats = cache.getStats()
        assertEquals(1L, stats["hitCount"])
        assertEquals(1L, stats["size"])
    }
}
