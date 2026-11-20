package com.federated.hospital

import com.federated.common.ast.*
import com.federated.common.visitor.DecompositionVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DecompositionVisitorTest {

    @Test
    fun `test node decomposition preserves query prefixes and filter clauses`() {
        val query = SparqlQuery(
            prefixes = listOf(PrefixDeclaration("ex", "http://example.org/health#")),
            selectVars = listOf(Var("cond"), Var("symptom")),
            clauses = listOf(
                BasicGraphPattern(
                    listOf(
                        TriplePattern(VariableTerm("p"), UriTerm("http://example.org/health#hasSymptom"), VariableTerm("symptom"))
                    )
                )
            ),
            filters = listOf(FilterClause("?symptom != 'None'"))
        )

        val decomposer = DecompositionVisitor("hospital-a", listOf("hospital-b", "hospital-c"))
        val decomposed = query.accept(decomposer)

        assertNotNull(decomposed)
        assertEquals(1, decomposed.prefixes.size)
        assertEquals(1, decomposed.filters.size)
    }
}
