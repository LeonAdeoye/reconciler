package com.reconciler.config.models

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter

data class QueryConfig(
    val count: Any, // String for SQL/N1QL, Map<String, Any> for MongoDB
    val parameters: Map<String, String>? = null,
    @get:JsonAnyGetter
    @get:JsonAnySetter
    val additionalProperties: MutableMap<String, Any> = mutableMapOf()
)

