package com.reconciler.datasource

import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import java.time.LocalDate

interface ReconciliationDataSource {
    fun getCount(entityType: EntityType, tradeDate: LocalDate, queryConfig: com.reconciler.config.models.QueryConfig): Long
    fun getDataSourceName(): String
    fun getDataSourceType(): DataSourceType
}

