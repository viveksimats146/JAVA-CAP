package com.federated.common.visitor

import com.federated.common.ast.SparqlQuery

interface QueryVisitor {
    fun visit(query: SparqlQuery): SparqlQuery
}
