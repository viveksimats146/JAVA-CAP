package com.federated.common.cache

import com.google.common.cache.CacheBuilder
import com.google.common.cache.Cache
import com.federated.common.rmi.FederatedQueryResponse
import java.util.concurrent.TimeUnit
import java.security.MessageDigest

class SubQueryCache(
    ttlSeconds: Long = 300,
    maxSize: Long = 1000
) {
    private val cache: Cache<String, FederatedQueryResponse> = CacheBuilder.newBuilder()
        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
        .maximumSize(maxSize)
        .recordStats()
        .build()

    fun generateCacheKey(peerNodeId: String, sparqlSubQuery: String): String {
        val normalized = sparqlSubQuery.trim().lowercase().replace("\\s+".toRegex(), " ")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(normalized.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        return "$peerNodeId:$hash"
    }

    fun get(peerNodeId: String, sparqlSubQuery: String): FederatedQueryResponse? {
        val key = generateCacheKey(peerNodeId, sparqlSubQuery)
        return cache.getIfPresent(key)
    }

    fun put(peerNodeId: String, sparqlSubQuery: String, response: FederatedQueryResponse) {
        val key = generateCacheKey(peerNodeId, sparqlSubQuery)
        cache.put(key, response)
    }

    fun invalidateAll() {
        cache.invalidateAll()
    }

    fun getStats(): Map<String, Any> {
        val stats = cache.stats()
        return mapOf(
            "hitCount" to stats.hitCount(),
            "missCount" to stats.missCount(),
            "hitRate" to stats.hitRate(),
            "evictionCount" to stats.evictionCount(),
            "size" to cache.size()
        )
    }
}
