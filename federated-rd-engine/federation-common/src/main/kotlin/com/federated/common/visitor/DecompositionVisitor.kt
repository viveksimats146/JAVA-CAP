package com.federated.common.visitor

import com.federated.common.ast.*

class DecompositionVisitor(
    val localNodeId: String,
    val peerNodeIds: List<String>
) : QueryVisitor {

    override fun visit(query: SparqlQuery): SparqlQuery {
        val localPatterns = mutableListOf<TriplePattern>()
        val peerPatternsMap = peerNodeIds.associateWith { mutableListOf<TriplePattern>() }.toMutableMap()

        query.clauses.forEach { clause ->
            when (clause) {
                is BasicGraphPattern -> {
                    clause.triplePatterns.forEach { pattern ->
                        val target = pattern.targetNodeId
                        if (target == null || target.equals(localNodeId, ignoreCase = true)) {
                            localPatterns.add(pattern)
                            // If target is null, also replicate pattern to peers for federated discovery
                            if (target == null) {
                                peerNodeIds.forEach { peerId ->
                                    peerPatternsMap[peerId]?.add(pattern)
                                }
                            }
                        } else {
                            if (peerPatternsMap.containsKey(target)) {
                                peerPatternsMap[target]?.add(pattern)
                            } else {
                                // Default fallback to local if peer unknown
                                localPatterns.add(pattern)
                            }
                        }
                    }
                }
                is ServiceClause -> {
                    // Service clause explicitly targets a URI/node
                    val serviceNode = clause.serviceUri.removePrefix("rmi://").removePrefix("http://").split("/").firstOrNull() ?: clause.serviceUri
                    val matchedPeer = peerNodeIds.firstOrNull { it.equals(serviceNode, ignoreCase = true) }
                    if (matchedPeer != null) {
                        peerPatternsMap[matchedPeer]?.addAll(clause.triplePatterns)
                    } else if (serviceNode.equals(localNodeId, ignoreCase = true)) {
                        localPatterns.addAll(clause.triplePatterns)
                    } else {
                        // Keep service clause as peer pattern under serviceUri
                        peerPatternsMap.getOrPut(clause.serviceUri) { mutableListOf() }.addAll(clause.triplePatterns)
                    }
                }
            }
        }

        val newClauses = mutableListOf<SparqlClause>()
        if (localPatterns.isNotEmpty()) {
            newClauses.add(BasicGraphPattern(localPatterns.distinct()))
        }

        peerPatternsMap.forEach { (peerId, patterns) ->
            if (patterns.isNotEmpty()) {
                newClauses.add(ServiceClause(serviceUri = peerId, triplePatterns = patterns.distinct()))
            }
        }

        return query.copy(clauses = newClauses)
    }

    fun extractLocalQuery(decomposedQuery: SparqlQuery): SparqlQuery {
        val localClauses = decomposedQuery.clauses.filterIsInstance<BasicGraphPattern>()
        return decomposedQuery.copy(clauses = localClauses)
    }

    fun extractPeerSubQueries(decomposedQuery: SparqlQuery): Map<String, SparqlQuery> {
        val result = mutableMapOf<String, SparqlQuery>()
        decomposedQuery.clauses.filterIsInstance<ServiceClause>().forEach { serviceClause ->
            val subQuery = decomposedQuery.copy(
                clauses = listOf(BasicGraphPattern(serviceClause.triplePatterns))
            )
            result[serviceClause.serviceUri] = subQuery
        }
        return result
    }
}
