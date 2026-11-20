package com.federated.hospital.graph

import com.federated.common.rmi.HospitalResult
import com.federated.hospital.logging.LogBuffer
import org.apache.jena.query.*
import org.apache.jena.tdb2.TDB2Factory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class LocalGraphManager(
    @Value("\${hospital.id:hospital-a}") private val hospitalId: String,
    @Value("\${hospital.dataset-provider-url:http://localhost:8080/datasets}") private val datasetProviderUrl: String,
    @Value("\${hospital.tdb2-path:./data/tdb2_hospital_a}") private val tdb2Path: String,
    private val restTemplate: RestTemplate,
    private val logBuffer: LogBuffer
) {
    private val logger = LoggerFactory.getLogger(LocalGraphManager::class.java)
    private lateinit var dataset: Dataset

    @PostConstruct
    fun init() {
        val dataDir = File(tdb2Path)
        dataDir.mkdirs()
        dataset = TDB2Factory.connectDataset(tdb2Path)
        logger.info("Connected to Jena TDB2 dataset at: $tdb2Path")
        logBuffer.addLog("INFO", "TDB2", "Connected to TDB2 dataset at $tdb2Path")
        try {
            reloadDatasetFromProvider()
        } catch (e: Exception) {
            logger.warn("Could not load dataset from provider on init: ${e.message}. Using existing TDB2 data if available.")
            logBuffer.addLog("WARN", "TDB2", "Dataset provider initial fetch failed: ${e.message}")
        }
    }

    @PreDestroy
    fun close() {
        if (::dataset.isInitialized) {
            try {
                dataset.close()
                logger.info("Closed Jena TDB2 dataset at: $tdb2Path")
            } catch (e: Exception) {
                logger.warn("Error closing TDB2 dataset: ${e.message}")
            }
        }
    }

    fun reloadDatasetFromProvider(): Boolean {
        val targetUrl = "$datasetProviderUrl/$hospitalId"
        logger.info("Fetching RDF dataset from: $targetUrl")
        logBuffer.addLog("INFO", "TDB2_RELOAD", "Fetching dataset from $targetUrl")

        var turtleContent: String? = null
        try {
            turtleContent = restTemplate.getForObject(targetUrl, String::class.java)
        } catch (e: Exception) {
            logger.warn("Remote dataset provider unreachable at $targetUrl (${e.message}). Falling back to bundled dataset...")
            logBuffer.addLog("WARN", "TDB2_RELOAD", "Remote provider unreachable, loading bundled dataset for $hospitalId")
        }

        if (turtleContent.isNullOrBlank()) {
            val resourceStream = javaClass.getResourceAsStream("/datasets/$hospitalId.ttl")
            if (resourceStream != null) {
                turtleContent = resourceStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                logger.info("Loaded bundled dataset for $hospitalId from classpath resources.")
                logBuffer.addLog("INFO", "TDB2_RELOAD", "Loaded bundled dataset for $hospitalId from classpath")
            } else {
                throw IllegalStateException("Received empty dataset response from $targetUrl and no bundled dataset found for $hospitalId")
            }
        }

        dataset.begin(ReadWrite.WRITE)
        try {
            val defaultModel = dataset.defaultModel
            defaultModel.removeAll() // Clear existing data for fresh reload

            val stream = ByteArrayInputStream(turtleContent.toByteArray(StandardCharsets.UTF_8))
            defaultModel.read(stream, null, "TURTLE")
            dataset.commit()

            val tripleCount = getTotalTripleCount()
            logger.info("Successfully loaded $tripleCount triples into Jena TDB2 for $hospitalId")
            logBuffer.addLog("INFO", "TDB2_RELOAD", "Successfully loaded $tripleCount triples into TDB2 for $hospitalId")
            return true
        } catch (e: Exception) {
            dataset.abort()
            logger.error("Failed to load dataset into Jena TDB2", e)
            logBuffer.addLog("ERROR", "TDB2_RELOAD", "Failed to load dataset: ${e.message}")
            throw e
        } finally {
            dataset.end()
        }
    }

    fun executeLocalSparql(sparqlQuery: String): List<HospitalResult> {
        val results = mutableListOf<HospitalResult>()
        dataset.begin(ReadWrite.READ)
        try {
            val query = QueryFactory.create(sparqlQuery)
            QueryExecutionFactory.create(query, dataset).use { qe ->
                val rs = qe.execSelect()
                while (rs.hasNext()) {
                    val qs = rs.nextSolution()
                    val bindings = mutableMapOf<String, String>()
                    val varNames = qs.varNames()
                    while (varNames.hasNext()) {
                        val v = varNames.next()
                        val node = qs.get(v)
                        if (node != null) {
                            bindings[v] = node.toString()
                        }
                    }

                    val patientStr = qs.get("patient")?.toString() ?: bindings["s"]
                    val condStr = qs.get("cond")?.toString() ?: qs.get("condition")?.toString() ?: bindings["o"] ?: "UnknownCondition"
                    val symptomStr = qs.get("symptom")?.toString() ?: bindings["symptom"] ?: "UnknownSymptom"
                    val ageBandStr = qs.get("ageBand")?.toString()
                    val reportedDateStr = qs.get("reportedDate")?.toString()

                    val cleanCond = condStr.removePrefix("http://example.org/health#")
                    val cleanSymptom = symptomStr.removePrefix("http://example.org/health#")
                    val cleanPatient = patientStr?.removePrefix("http://example.org/health#")

                    results.add(
                        HospitalResult(
                            patientId = cleanPatient,
                            condition = cleanCond,
                            symptom = cleanSymptom,
                            hospitalId = hospitalId,
                            ageBand = ageBandStr,
                            reportedDate = reportedDateStr,
                            rawBindings = bindings
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error executing local SPARQL query: ${e.message}", e)
            logBuffer.addLog("ERROR", "SPARQL_EXEC", "Local query execution failed: ${e.message}")
        } finally {
            dataset.end()
        }
        return results
    }

    fun getTotalTripleCount(): Long {
        var count = 0L
        dataset.begin(ReadWrite.READ)
        try {
            count = dataset.defaultModel.size()
        } finally {
            dataset.end()
        }
        return count
    }
}
