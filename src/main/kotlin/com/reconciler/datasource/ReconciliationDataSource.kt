package com.reconciler.datasource

import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType

interface ReconciliationDataSource {
    fun getCount(entityType: EntityType, parameters: Map<String, String>, queryConfig: com.reconciler.config.models.QueryConfig): Long
    fun getDataSourceName(): String
    fun getDataSourceType(): DataSourceType
}

