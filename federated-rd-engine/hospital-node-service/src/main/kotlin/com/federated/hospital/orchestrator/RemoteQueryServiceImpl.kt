package com.federated.hospital.orchestrator

import com.federated.common.rmi.FederatedQueryRequest
import com.federated.common.rmi.FederatedQueryResponse
import com.federated.common.rmi.NodeHealth
import com.federated.common.rmi.RemoteQueryService
import com.federated.hospital.graph.LocalGraphManager
import com.federated.hospital.logging.LogBuffer
import org.slf4j.LoggerFactory
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

class RemoteQueryServiceImpl(
    private val nodeId: String,
    private val localGraphManager: LocalGraphManager,
    private val logBuffer: LogBuffer,
    port: Int
) : UnicastRemoteObject(port), RemoteQueryService {

    private val logger = LoggerFactory.getLogger(RemoteQueryServiceImpl::class.java)
    private val startTimeMs = System.currentTimeMillis()

    @Throws(RemoteException::class)
    override fun executeSubQuery(request: FederatedQueryRequest): FederatedQueryResponse {
        val start = System.currentTimeMillis()
        logger.info("Executing RMI sub-query from ${request.originatingNodeId} (QueryId: ${request.queryId})")
        logBuffer.addLog("INFO", "RMI_SUBQUERY", "Received query ${request.queryId} from ${request.originatingNodeId}")

        return try {
            val results = localGraphManager.executeLocalSparql(request.sparqlSubQuery)
            val execTime = System.currentTimeMillis() - start
            logger.info("Executed RMI sub-query in ${execTime}ms returning ${results.size} results")
            logBuffer.addLog("INFO", "RMI_SUBQUERY", "Completed query ${request.queryId} in ${execTime}ms with ${results.size} results")

            FederatedQueryResponse(
                queryId = request.queryId,
                respondingNodeId = nodeId,
                success = true,
                executionTimeMs = execTime,
                results = results
            )
        } catch (e: Exception) {
            val execTime = System.currentTimeMillis() - start
            logger.error("Error executing RMI sub-query: ${e.message}", e)
            logBuffer.addLog("ERROR", "RMI_SUBQUERY", "Failed query ${request.queryId}: ${e.message}")

            FederatedQueryResponse(
                queryId = request.queryId,
                respondingNodeId = nodeId,
                success = false,
                errorMessage = e.message ?: "Internal sub-query execution error",
                executionTimeMs = execTime,
                results = emptyList()
            )
        }
    }

    @Throws(RemoteException::class)
    override fun getHealthStatus(): NodeHealth {
        val totalTriples = localGraphManager.getTotalTripleCount()
        val uptimeSec = (System.currentTimeMillis() - startTimeMs) / 1000
        return NodeHealth(
            nodeId = nodeId,
            status = "UP",
            totalTriples = totalTriples,
            uptimeSeconds = uptimeSec
        )
    }
}
