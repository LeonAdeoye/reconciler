package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.datasource.ReconciliationDataSource
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PostConstruct

@Component
class DataSourceFactoryRegistry(
    private val factories: List<DataSourceFactory>
) {
    private val factoryMap = ConcurrentHashMap<DataSourceType, DataSourceFactory>()
    private val dataSourceCache = ConcurrentHashMap<String, ReconciliationDataSource>()

    @PostConstruct
    fun init() {
        factories.forEach { factory ->
            DataSourceType.values().forEach { type ->
                if (factory.supports(type)) {
                    factoryMap[type] = factory
                }
            }
        }
    }

    fun createDataSource(config: DataSourceConfig): ReconciliationDataSource {
        // Check cache first
        val cached = dataSourceCache[config.name]
        if (cached != null) {
            return cached
        }

        val factory = factoryMap[config.type]
            ?: throw IllegalArgumentException("No factory found for data source type: ${config.type}")

        val dataSource = factory.createDataSource(config)
        dataSourceCache[config.name] = dataSource
        return dataSource
    }

    fun clearCache() {
        dataSourceCache.clear()
    }
}

