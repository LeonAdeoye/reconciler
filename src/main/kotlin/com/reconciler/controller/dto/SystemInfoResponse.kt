package com.reconciler.controller.dto

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.SourceSystemConfig

data class SystemInfoResponse(
    val name: String,
    val dataSources: List<DataSourceInfo>
)

data class DataSourceInfo(
    val name: String,
    val type: String,
    val entityTypes: List<String>
)

fun SourceSystemConfig.toSystemInfoResponse(): SystemInfoResponse {
    return SystemInfoResponse(
        name = this.name,
        dataSources = this.dataSources.map { ds ->
            DataSourceInfo(
                name = ds.name,
                type = ds.type.name,
                entityTypes = ds.entityTypes.map { it.name }
            )
        }
    )
}

