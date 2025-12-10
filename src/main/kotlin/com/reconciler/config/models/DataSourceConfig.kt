package com.reconciler.config.models

data class DataSourceConfig(
    val type: DataSourceType,
    val name: String,
    val connectionConfig: Map<String, Any>,
    val entityTypes: List<EntityType>,
    val queries: Map<String, QueryConfig>? = null // Key is EntityType name
)

