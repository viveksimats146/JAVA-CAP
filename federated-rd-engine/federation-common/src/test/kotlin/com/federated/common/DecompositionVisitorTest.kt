package com.federated.common

import com.federated.common.ast.*
import com.federated.common.visitor.DecompositionVisitor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DecompositionVisitorTest {

    @Test
    fun `test DecompositionVisitor splits queries into local and peer service clauses`() {
        val visitor = DecompositionVisitor(
            localNodeId = "hospital-a",
            peerNodeIds = listOf("hospital-b", "hospital-c")
        )

        val query = SparqlQuery(
            selectVars = listOf(Var("cond"), Var("symptom")),
            clauses = listOf(
                BasicGraphPattern(
                    listOf(
                        TriplePattern(VariableTerm("patient"), UriTerm("http://example.org/health#diagnosedWith"), VariableTerm("cond"), targetNodeId = "hospital-a"),
                        TriplePattern(VariableTerm("patient"), UriTerm("http://example.org/health#hasSymptom"), VariableTerm("symptom"), targetNodeId = "hospital-b"),
                        TriplePattern(VariableTerm("patient"), UriTerm("http://example.org/health#reportedBy"), UriTerm("http://example.org/health#HospitalC"), targetNodeId = "hospital-c")
                    )
                )
            )
        )

        val decomposed = query.accept(visitor)
        assertNotNull(decomposed)

        val localQuery = visitor.extractLocalQuery(decomposed)
        assertEquals(1, localQuery.clauses.size)
        val localBgp = localQuery.clauses[0] as BasicGraphPattern
        assertEquals(1, localBgp.triplePatterns.size)
        assertEquals("hospital-a", localBgp.triplePatterns[0].targetNodeId)

        val peerSubQueries = visitor.extractPeerSubQueries(decomposed)
        assertEquals(2, peerSubQueries.size)
        assertTrue(peerSubQueries.containsKey("hospital-b"))
        assertTrue(peerSubQueries.containsKey("hospital-c"))
    }
}
