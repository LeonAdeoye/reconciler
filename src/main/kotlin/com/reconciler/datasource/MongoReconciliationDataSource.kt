package com.reconciler.datasource

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.bson.Document
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MongoReconciliationDataSource(
    private val dataSourceName: String,
    private val mongoClient: MongoClient,
    private val database: MongoDatabase,
    private val collectionMap: Map<String, String>
) : ReconciliationDataSource {

    override fun getCount(
        entityType: EntityType,
        tradeDate: LocalDate,
        queryConfig: QueryConfig
    ): Long {
        val collectionName = collectionMap[entityType.name]
            ?: throw IllegalArgumentException("No collection mapped for entity type: ${entityType.name}")
        
        val collection = database.getCollection(collectionName)
        
        val query = queryConfig.count
        val filter = when (query) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val filterDoc = Document(query as Map<String, Any>)
                replacePlaceholders(filterDoc, tradeDate)
                filterDoc
            }
            is String -> {
                // If it's a string, try to parse as JSON
                Document.parse(query)
            }
            else -> throw IllegalArgumentException("MongoDB query must be a Map or JSON string")
        }

        return collection.countDocuments(filter)
    }

    override fun getDataSourceName(): String = dataSourceName

    override fun getDataSourceType(): DataSourceType = DataSourceType.MONGODB

    private fun replacePlaceholders(doc: Document, tradeDate: LocalDate) {
        val dateStr = tradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        replacePlaceholdersRecursive(doc, dateStr)
    }

    private fun replacePlaceholdersRecursive(obj: Any, dateStr: String): Any {
        return when (obj) {
            is Document -> {
                obj.forEach { (key, value) ->
                    obj[key] = replacePlaceholdersRecursive(value, dateStr)
                }
                obj
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = obj as MutableMap<String, Any>
                map.forEach { (key, value) ->
                    map[key] = replacePlaceholdersRecursive(value, dateStr)
                }
                map
            }
            is List<*> -> {
                obj.map { replacePlaceholdersRecursive(it!!, dateStr) }
            }
            is String -> {
                when {
                    obj == "?tradeDate" -> dateStr
                    obj.contains("?tradeDate") -> obj.replace("?tradeDate", dateStr)
                    else -> obj
                }
            }
            else -> obj
        }
    }
}

