package com.federated.hospital.orchestrator

import com.federated.hospital.graph.LocalGraphManager
import com.federated.hospital.logging.LogBuffer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class RmiServerManager(
    @Value("\${hospital.id:hospital-a}") private val hospitalId: String,
    @Value("\${hospital.rmi-port:1099}") private val rmiPort: Int,
    private val localGraphManager: LocalGraphManager,
    private val logBuffer: LogBuffer
) {
    private val logger = LoggerFactory.getLogger(RmiServerManager::class.java)
    private var registry: Registry? = null
    private var remoteService: RemoteQueryServiceImpl? = null

    @PostConstruct
    fun startRmiServer() {
        try {
            logger.info("Starting RMI Registry on port $rmiPort for node $hospitalId...")
            logBuffer.addLog("INFO", "RMI_SERVER", "Starting RMI registry on port $rmiPort")

            registry = try {
                LocateRegistry.createRegistry(rmiPort)
            } catch (e: Exception) {
                LocateRegistry.getRegistry(rmiPort)
            }

            remoteService = RemoteQueryServiceImpl(
                nodeId = hospitalId,
                localGraphManager = localGraphManager,
                logBuffer = logBuffer,
                port = 0
            )

            registry?.rebind("RemoteQueryService", remoteService)
            logger.info("Successfully bound 'RemoteQueryService' in RMI registry on port $rmiPort")
            logBuffer.addLog("INFO", "RMI_SERVER", "Bound 'RemoteQueryService' on port $rmiPort")
        } catch (e: Exception) {
            logger.error("Failed to start RMI server on port $rmiPort: ${e.message}", e)
            logBuffer.addLog("ERROR", "RMI_SERVER", "RMI server startup failed: ${e.message}")
        }
    }

    @PreDestroy
    fun stopRmiServer() {
        try {
            registry?.unbind("RemoteQueryService")
            logger.info("Unbound RemoteQueryService from RMI registry")
        } catch (e: Exception) {
            logger.warn("Error stopping RMI server: ${e.message}")
        }
    }
}
