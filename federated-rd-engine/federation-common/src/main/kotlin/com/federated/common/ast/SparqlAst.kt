package com.federated.common.ast

import com.federated.common.visitor.QueryVisitor
import java.io.Serializable

sealed interface Term : Serializable {
    val value: String
}

data class UriTerm(override val value: String) : Term {
    companion object { private const val serialVersionUID = 1101L }
}

data class LiteralTerm(override val value: String) : Term {
    companion object { private const val serialVersionUID = 1102L }
}

data class VariableTerm(override val value: String) : Term {
    companion object { private const val serialVersionUID = 1103L }
}

data class Var(val name: String) : Serializable {
    companion object { private const val serialVersionUID = 1104L }
}

data class TriplePattern(
    val subject: Term,
    val predicate: Term,
    val objectTerm: Term,
    val targetNodeId: String? = null
) : Serializable {
    companion object { private const val serialVersionUID = 1105L }

    fun toSparqlString(): String {
        return "${termToString(subject)} ${termToString(predicate)} ${termToString(objectTerm)} ."
    }

    private fun termToString(term: Term): String = when (term) {
        is UriTerm -> if (term.value.startsWith("http") || term.value.startsWith("urn:")) "<${term.value}>" else term.value
        is LiteralTerm -> "\"${term.value}\""
        is VariableTerm -> "?${term.value}"
    }
}

data class FilterClause(val expression: String) : Serializable {
    companion object { private const val serialVersionUID = 1106L }
}

data class PrefixDeclaration(val prefix: String, val iri: String) : Serializable {
    companion object { private const val serialVersionUID = 1107L }
}

sealed interface SparqlClause : Serializable

data class BasicGraphPattern(
    val triplePatterns: List<TriplePattern>
) : SparqlClause {
    companion object { private const val serialVersionUID = 1108L }
}

data class ServiceClause(
    val serviceUri: String,
    val triplePatterns: List<TriplePattern>
) : SparqlClause {
    companion object { private const val serialVersionUID = 1109L }
}

data class SparqlQuery(
    val prefixes: List<PrefixDeclaration> = emptyList(),
    val selectVars: List<Var> = emptyList(),
    val clauses: List<SparqlClause> = emptyList(),
    val filters: List<FilterClause> = emptyList()
) : Serializable {
    companion object { private const val serialVersionUID = 1110L }

    fun accept(visitor: QueryVisitor): SparqlQuery = visitor.visit(this)

    fun toSparqlString(): String {
        val prefixLines = prefixes.joinToString("\n") { "PREFIX ${it.prefix}: <${it.iri}>" }
        val selectClause = if (selectVars.isEmpty()) "SELECT *" else "SELECT " + selectVars.joinToString(" ") { "?${it.name}" }
        val bodyLines = mutableListOf<String>()

        clauses.forEach { clause ->
            when (clause) {
                is BasicGraphPattern -> clause.triplePatterns.forEach { bodyLines.add(it.toSparqlString()) }
                is ServiceClause -> {
                    bodyLines.add("SERVICE <${clause.serviceUri}> {")
                    clause.triplePatterns.forEach { bodyLines.add("  ${it.toSparqlString()}") }
                    bodyLines.add("}")
                }
            }
        }
        filters.forEach { bodyLines.add("FILTER(${it.expression})") }

        val body = bodyLines.joinToString("\n  ")
        return if (prefixLines.isBlank()) "$selectClause WHERE {\n  $body\n}" else "$prefixLines\n$selectClause WHERE {\n  $body\n}"
    }
}
