package com.federated.common.visitor

import com.federated.common.ast.*

class CostEstimationVisitor : QueryVisitor {

    var estimatedCost: Double = 0.0
        private set

    override fun visit(query: SparqlQuery): SparqlQuery {
        var cost = 0.0

        query.clauses.forEach { clause ->
            when (clause) {
                is BasicGraphPattern -> {
                    cost += clause.triplePatterns.size * 10.0
                    clause.triplePatterns.forEach { tp ->
                        if (tp.subject is VariableTerm) cost += 5.0
                        if (tp.predicate is VariableTerm) cost += 15.0 // High penalty for unbound predicate
                        if (tp.objectTerm is VariableTerm) cost += 5.0
                    }
                }
                is ServiceClause -> {
                    cost += 25.0 // Network RMI latency overhead
                    cost += clause.triplePatterns.size * 12.0
                    clause.triplePatterns.forEach { tp ->
                        if (tp.subject is VariableTerm) cost += 5.0
                        if (tp.predicate is VariableTerm) cost += 15.0
                        if (tp.objectTerm is VariableTerm) cost += 5.0
                    }
                }
            }
        }

        cost += query.filters.size * 15.0
        if (query.selectVars.isEmpty()) cost += 20.0 // SELECT * penalty

        this.estimatedCost = cost
        return query
    }

    fun estimateCost(query: SparqlQuery): Double {
        visit(query)
        return estimatedCost
    }
}
