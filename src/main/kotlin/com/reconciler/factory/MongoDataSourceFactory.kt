package com.reconciler.factory

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.datasource.MongoReconciliationDataSource
import org.springframework.stereotype.Component

@Component
class MongoDataSourceFactory : DataSourceFactory {
    override fun supports(type: DataSourceType): Boolean {
        return type == DataSourceType.MONGODB
    }

    override fun createDataSource(config: DataSourceConfig): MongoReconciliationDataSource {
        val connectionConfig = config.connectionConfig
        
        val uri = connectionConfig["uri"] as? String
            ?: throw IllegalArgumentException("MongoDB URI not specified")
        
        val databaseName = connectionConfig["database"] as? String
            ?: throw IllegalArgumentException("MongoDB database not specified")

        val mongoClient: MongoClient = MongoClients.create(uri)
        val database: MongoDatabase = mongoClient.getDatabase(databaseName)

        return MongoReconciliationDataSource(
            dataSourceName = config.name,
            mongoClient = mongoClient,
            database = database,
            collectionMap = buildCollectionMap(config, connectionConfig)
        )
    }

    private fun buildCollectionMap(
        config: DataSourceConfig,
        connectionConfig: Map<String, Any>
    ): Map<String, String> {
        val collectionMap = mutableMapOf<String, String>()
        
        config.entityTypes.forEach { entityType ->
            val collectionKey = "${entityType.name.lowercase()}_collection"
            val collectionName = connectionConfig[collectionKey] as? String
                ?: connectionConfig["collection"] as? String
                ?: entityType.name.lowercase()
            collectionMap[entityType.name] = collectionName
        }
        
        return collectionMap
    }
}

