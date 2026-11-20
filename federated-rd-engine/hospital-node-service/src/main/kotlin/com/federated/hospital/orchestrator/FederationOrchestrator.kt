package com.federated.hospital.orchestrator

import com.federated.common.ast.*
import com.federated.common.cache.SubQueryCache
import com.federated.common.rmi.*
import com.federated.common.visitor.CostEstimationVisitor
import com.federated.common.visitor.DecompositionVisitor
import com.federated.hospital.alignment.ResultAlignmentEngine
import com.federated.hospital.graph.LocalGraphManager
import com.federated.hospital.logging.LogBuffer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.rmi.registry.LocateRegistry
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Component
class FederationOrchestrator(
    @Value("\${hospital.id:hospital-a}") private val hospitalId: String,
    @Value("\${hospital.peer-list:hospital-b:1100,hospital-c:1101}") private val peerListString: String,
    private val localGraphManager: LocalGraphManager,
    private val resultAlignmentEngine: ResultAlignmentEngine,
    private val logBuffer: LogBuffer
) {
    private val logger = LoggerFactory.getLogger(FederationOrchestrator::class.java)
    val subQueryCache = SubQueryCache(ttlSeconds = 300, maxSize = 1000)

    private val peerConfig: List<PeerNodeConfig> by lazy {
        if (peerListString.isBlank()) emptyList() else {
            peerListString.split(",").mapNotNull { entry ->
                val parts = entry.trim().split(":")
                if (parts.size >= 2) {
                    val hostOrId = parts[0]
                    val port = parts[1].toIntOrNull() ?: 1099
                    val nodeId = if (hostOrId.contains(".")) hostOrId.split(".")[0] else hostOrId
                    PeerNodeConfig(nodeId = nodeId, host = hostOrId, port = port)
                } else null
            }
        }
    }

    data class PeerNodeConfig(val nodeId: String, val host: String, val port: Int)

    fun executeFederatedQuery(sparqlString: String, maxResults: Int = 100): FederatedQueryResult {
        val queryId = UUID.randomUUID().toString().take(8)
        val startTime = System.currentTimeMillis()
        logBuffer.addLog("INFO", "FEDERATION", "Starting federated query [$queryId]")

        val astQuery = parseSparqlToAst(sparqlString)

        val costVisitor = CostEstimationVisitor()
        val costScore = costVisitor.estimateCost(astQuery)
        logBuffer.addLog("INFO", "AST_COST", "Query [$queryId] estimated complexity cost score: $costScore")

        val peerIds = peerConfig.map { it.nodeId }
        val decomposer = DecompositionVisitor(localNodeId = hospitalId, peerNodeIds = peerIds)
        val decomposedQuery = astQuery.accept(decomposer)

        val localSubQuery = decomposer.extractLocalQuery(decomposedQuery)
        val peerSubQueries = decomposer.extractPeerSubQueries(decomposedQuery)

        logBuffer.addLog("INFO", "DECOMPOSITION", "Local clauses: ${localSubQuery.clauses.size}, Peer sub-queries: ${peerSubQueries.size}")

        val allHospitalResults = mutableListOf<HospitalResult>()
        val peerStatuses = mutableListOf<NodeHealth>()

        // 1. Execute local query
        val localStart = System.currentTimeMillis()
        val localResults = localGraphManager.executeLocalSparql(localSubQuery.toSparqlString())
        val localExecTime = System.currentTimeMillis() - localStart
        allHospitalResults.addAll(localResults)
        logBuffer.addLog("INFO", "LOCAL_EXEC", "Local execution returned ${localResults.size} triples in ${localExecTime}ms")

        peerStatuses.add(
            NodeHealth(
                nodeId = hospitalId,
                status = "UP",
                totalTriples = localGraphManager.getTotalTripleCount(),
                uptimeSeconds = 3600
            )
        )

        // 2. Execute peer RMI calls concurrently with 3s timeout
        val futures = peerConfig.map { peer ->
            CompletableFuture.supplyAsync {
                val subQueryStr = peerSubQueries[peer.nodeId]?.toSparqlString() ?: sparqlString

                // Check cache first
                val cachedResponse = subQueryCache.get(peer.nodeId, subQueryStr)
                if (cachedResponse != null) {
                    logBuffer.addLog("INFO", "CACHE_HIT", "Sub-query cache hit for peer ${peer.nodeId}")
                    Pair(peer, cachedResponse)
                } else {
                    logBuffer.addLog("INFO", "CACHE_MISS", "Cache miss for peer ${peer.nodeId}. Initiating RMI call...")
                    val peerResponse = callPeerRmi(peer, queryId, subQueryStr, maxResults)
                    if (peerResponse.success) {
                        subQueryCache.put(peer.nodeId, subQueryStr, peerResponse)
                    }
                    Pair(peer, peerResponse)
                }
            }
        }

        peerConfig.zip(futures).forEach { (peer, future) ->
            try {
                val (_, response) = future.get(3, TimeUnit.SECONDS)
                if (response.success) {
                    allHospitalResults.addAll(response.results)
                    peerStatuses.add(
                        NodeHealth(
                            nodeId = peer.nodeId,
                            status = "UP",
                            totalTriples = response.results.size.toLong(),
                            uptimeSeconds = 3600
                        )
                    )
                } else {
                    logBuffer.addLog("WARN", "PEER_FAILURE", "Peer ${peer.nodeId} returned error: ${response.errorMessage}")
                    peerStatuses.add(
                        NodeHealth(nodeId = peer.nodeId, status = "DEGRADED", totalTriples = 0, uptimeSeconds = 0)
                    )
                }
            } catch (e: Exception) {
                logger.warn("Peer execution timed out or failed: ${e.message}")
                logBuffer.addLog("WARN", "PEER_TIMEOUT", "Peer RMI call timed out / unreachable: ${e.message}")
                peerStatuses.add(
                    NodeHealth(nodeId = peer.nodeId, status = "DOWN", totalTriples = 0, uptimeSeconds = 0)
                )
            }
        }

        // 3. Result Alignment and Ranking
        val rankedResults = resultAlignmentEngine.alignAndRankResults(allHospitalResults)
        val totalExecTime = System.currentTimeMillis() - startTime

        logBuffer.addLog("INFO", "FEDERATION", "Query [$queryId] completed in ${totalExecTime}ms producing ${rankedResults.size} ranked results")

        return FederatedQueryResult(
            queryId = queryId,
            executionTimeMs = totalExecTime,
            rankedResults = rankedResults,
            nodeStatuses = peerStatuses,
            rawResultsCount = allHospitalResults.size
        )
    }

    private fun callPeerRmi(
        peer: PeerNodeConfig,
        queryId: String,
        subQueryStr: String,
        maxResults: Int
    ): FederatedQueryResponse {
        val start = System.currentTimeMillis()
        return try {
            val registry = LocateRegistry.getRegistry(peer.host, peer.port)
            val service = registry.lookup("RemoteQueryService") as RemoteQueryService
            val req = FederatedQueryRequest(
                queryId = queryId,
                sparqlSubQuery = subQueryStr,
                originatingNodeId = hospitalId,
                maxResults = maxResults
            )
            service.executeSubQuery(req)
        } catch (e: Exception) {
            val execTime = System.currentTimeMillis() - start
            FederatedQueryResponse(
                queryId = queryId,
                respondingNodeId = peer.nodeId,
                success = false,
                errorMessage = "RMI Connection Error: ${e.message}",
                executionTimeMs = execTime,
                results = emptyList()
            )
        }
    }

    private fun parseSparqlToAst(sparql: String): SparqlQuery {
        // Construct AST representation for incoming query
        val triplePatterns = mutableListOf<TriplePattern>()

        val cleaned = sparql.replace(Regex("PREFIX.*?\n", RegexOption.IGNORE_CASE), "")
            .replace(Regex("SELECT.*WHERE\\s*\\{", RegexOption.IGNORE_CASE), "")
            .replace("}", "")
            .trim()

        val lines = cleaned.split("\n")
        lines.forEach { line ->
            val trimmed = line.trim().removeSuffix(".").trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val tokens = trimmed.split("\\s+".toRegex())
                if (tokens.size >= 3) {
                    val sToken = parseTerm(tokens[0])
                    val pToken = parseTerm(tokens[1])
                    val oToken = parseTerm(tokens[2])
                    triplePatterns.add(TriplePattern(sToken, pToken, oToken))
                }
            }
        }

        val fallbackPatterns = if (triplePatterns.isEmpty()) {
            listOf(
                TriplePattern(VariableTerm("patient"), UriTerm("http://example.org/health#diagnosedWith"), VariableTerm("cond")),
                TriplePattern(VariableTerm("patient"), UriTerm("http://example.org/health#hasSymptom"), VariableTerm("symptom"))
            )
        } else triplePatterns

        return SparqlQuery(
            prefixes = listOf(PrefixDeclaration("ex", "http://example.org/health#")),
            selectVars = listOf(Var("patient"), Var("cond"), Var("symptom")),
            clauses = listOf(BasicGraphPattern(fallbackPatterns))
        )
    }

    private fun parseTerm(str: String): Term {
        val t = str.trim()
        return when {
            t.startsWith("?") -> VariableTerm(t.removePrefix("?"))
            t.startsWith("<") && t.endsWith(">") -> UriTerm(t.removeSurrounding("<", ">"))
            t.contains(":") -> UriTerm("http://example.org/health#" + t.substringAfter(":"))
            else -> LiteralTerm(t.removeSurrounding("\""))
        }
    }

    fun getNodeStatuses(): List<NodeHealth> {
        val statuses = mutableListOf<NodeHealth>()
        statuses.add(
            NodeHealth(
                nodeId = hospitalId,
                status = "UP",
                totalTriples = localGraphManager.getTotalTripleCount(),
                uptimeSeconds = 3600
            )
        )

        peerConfig.forEach { peer ->
            try {
                val registry = LocateRegistry.getRegistry(peer.host, peer.port)
                val service = registry.lookup("RemoteQueryService") as RemoteQueryService
                statuses.add(service.getHealthStatus())
            } catch (e: Exception) {
                statuses.add(NodeHealth(nodeId = peer.nodeId, status = "DOWN", totalTriples = 0, uptimeSeconds = 0))
            }
        }
        return statuses
    }
}

data class FederatedQueryResult(
    val queryId: String,
    val executionTimeMs: Long,
    val rankedResults: List<RankedResult>,
    val nodeStatuses: List<NodeHealth>,
    val rawResultsCount: Int
)
