package com.federated.hospital.alignment

import com.federated.common.rmi.HospitalResult
import com.federated.common.rmi.RankedResult
import org.springframework.stereotype.Component

@Component
class ResultAlignmentEngine {

    fun alignAndRankResults(results: List<HospitalResult>): List<RankedResult> {
        if (results.isEmpty()) return emptyList()

        // Group by (condition, symptom) pair
        val grouped = results.groupBy { Pair(it.condition, it.symptom) }

        val rankedList = grouped.map { (pair, items) ->
            val condition = pair.first
            val symptom = pair.second
            val contributingHospitals = items.map { it.hospitalId }.distinct().sorted()
            val totalCases = items.size

            // Ranking score formula: 10.0 points per distinct contributing hospital + 1.0 point per case
            val score = (contributingHospitals.size * 10.0) + (totalCases * 1.0)

            RankedResult(
                condition = condition,
                symptom = symptom,
                score = score,
                contributingHospitals = contributingHospitals,
                totalCases = totalCases,
                hospitalDetails = items
            )
        }

        // Sort descending by score, then by hospital count, then by condition name
        return rankedList.sortedWith(
            compareByDescending<RankedResult> { it.score }
                .thenByDescending { it.contributingHospitals.size }
                .thenByDescending { it.totalCases }
                .thenBy { it.condition }
        )
    }
}
