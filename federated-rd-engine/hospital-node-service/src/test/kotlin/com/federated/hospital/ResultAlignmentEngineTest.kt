package com.federated.hospital

import com.federated.common.rmi.HospitalResult
import com.federated.hospital.alignment.ResultAlignmentEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultAlignmentEngineTest {

    @Test
    fun `test ResultAlignmentEngine merges, deduplicates, and ranks by contributing hospital count`() {
        val engine = ResultAlignmentEngine()

        val results = listOf(
            // COVID-19 with Fever present in Hospital A, Hospital B, Hospital C
            HospitalResult(condition = "COVID-19", symptom = "Fever", hospitalId = "hospital-a"),
            HospitalResult(condition = "COVID-19", symptom = "Fever", hospitalId = "hospital-b"),
            HospitalResult(condition = "COVID-19", symptom = "Fever", hospitalId = "hospital-c"),

            // Influenza with Cough present in Hospital A and Hospital B
            HospitalResult(condition = "Influenza", symptom = "Cough", hospitalId = "hospital-a"),
            HospitalResult(condition = "Influenza", symptom = "Cough", hospitalId = "hospital-b"),

            // Asthma with Wheezing present only in Hospital C
            HospitalResult(condition = "Asthma", symptom = "Wheezing", hospitalId = "hospital-c")
        )

        val ranked = engine.alignAndRankResults(results)

        assertEquals(3, ranked.size)

        // COVID-19 + Fever has 3 hospitals (A, B, C) -> highest score
        val topResult = ranked[0]
        assertEquals("COVID-19", topResult.condition)
        assertEquals("Fever", topResult.symptom)
        assertEquals(3, topResult.contributingHospitals.size)
        assertTrue(topResult.contributingHospitals.containsAll(listOf("hospital-a", "hospital-b", "hospital-c")))

        // Influenza + Cough has 2 hospitals (A, B) -> second rank
        val secondResult = ranked[1]
        assertEquals("Influenza", secondResult.condition)
        assertEquals(2, secondResult.contributingHospitals.size)

        // Asthma + Wheezing has 1 hospital (C) -> third rank
        val thirdResult = ranked[2]
        assertEquals("Asthma", thirdResult.condition)
        assertEquals(1, thirdResult.contributingHospitals.size)
    }
}
