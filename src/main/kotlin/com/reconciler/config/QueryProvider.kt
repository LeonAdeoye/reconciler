package com.reconciler.config

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.springframework.stereotype.Service

@Service
class QueryProvider {
    fun getCountQuery(dataSourceConfig: DataSourceConfig, entityType: EntityType): QueryConfig {
        val queries = dataSourceConfig.queries
            ?: throw IllegalArgumentException("No queries defined for data source: ${dataSourceConfig.name}")
        
        val entityTypeName = entityType.name
        val queryConfig = queries[entityTypeName]
            ?: throw IllegalArgumentException(
                "No count query found for entity type $entityTypeName in data source ${dataSourceConfig.name}"
            )
        
        return queryConfig
    }
}

