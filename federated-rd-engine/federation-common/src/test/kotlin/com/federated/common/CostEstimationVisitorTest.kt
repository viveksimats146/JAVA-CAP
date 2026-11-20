package com.federated.common

import com.federated.common.ast.*
import com.federated.common.visitor.CostEstimationVisitor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CostEstimationVisitorTest {

    @Test
    fun `test CostEstimationVisitor penalizes unbound variables and network service clauses`() {
        val simpleQuery = SparqlQuery(
            selectVars = listOf(Var("patient")),
            clauses = listOf(
                BasicGraphPattern(
                    listOf(
                        TriplePattern(UriTerm("http://example.org/p1"), UriTerm("http://example.org/has"), LiteralTerm("val"))
                    )
                )
            )
        )

        val complexQuery = SparqlQuery(
            selectVars = emptyList(), // SELECT * penalty
            clauses = listOf(
                ServiceClause(
                    serviceUri = "hospital-b",
                    triplePatterns = listOf(
                        TriplePattern(VariableTerm("s"), VariableTerm("p"), VariableTerm("o"))
                    )
                )
            ),
            filters = listOf(FilterClause("?o > 10"))
        )

        val visitor = CostEstimationVisitor()
        val simpleCost = visitor.estimateCost(simpleQuery)
        val complexCost = visitor.estimateCost(complexQuery)

        assertTrue(complexCost > simpleCost, "Complex query with RMI and unbound variables should have higher cost")
    }
}
