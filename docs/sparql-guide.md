# SPARQL Query Decomposition & Ontology Guide

## Overview

The `federation-common` module parses SPARQL queries into an Abstract Syntax Tree (AST) and decomposes global queries into localized sub-queries executable against individual Jena TDB2 triple stores.

## Supported SPARQL Syntax

### Example Query
```sparql
PREFIX med: <http://example.org/med#>
SELECT ?condition ?symptom
WHERE {
  ?patient med:hasCondition ?condition .
  ?patient med:hasSymptom ?symptom .
}
LIMIT 50
```

## Decomposition Pipeline

1. **Parser & AST Construction**: SPARQL string is transformed into `SparqlAst.Select` with child AST nodes.
2. **Decomposition (`DecompositionVisitor`)**: Splits graph patterns based on predicate namespace mapping.
3. **Cost Estimation (`CostEstimationVisitor`)**: Ranks execution paths based on triple count heuristics and network latency.
4. **Caching (`SubQueryCache`)**: Results of common sub-trees are served from LRU cache.
