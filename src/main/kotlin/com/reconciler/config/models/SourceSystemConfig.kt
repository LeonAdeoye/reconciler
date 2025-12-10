package com.reconciler.config.models

data class SourceSystemConfig(
    val name: String,
    val dataSources: List<DataSourceConfig>
)

