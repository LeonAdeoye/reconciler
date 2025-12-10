package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.datasource.ReconciliationDataSource

interface DataSourceFactory {
    fun createDataSource(config: DataSourceConfig): ReconciliationDataSource
    fun supports(type: DataSourceType): Boolean
}

