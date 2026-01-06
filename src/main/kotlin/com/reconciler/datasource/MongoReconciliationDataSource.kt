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
        parameters: Map<String, String>,
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
                replacePlaceholders(filterDoc, parameters, queryConfig.parameters ?: emptyMap())
                filterDoc
            }
            is String -> {
                // If it's a string, try to parse as JSON
                val doc = Document.parse(query)
                replacePlaceholders(doc, parameters, queryConfig.parameters ?: emptyMap())
                doc
            }
            else -> throw IllegalArgumentException("MongoDB query must be a Map or JSON string")
        }

        return collection.countDocuments(filter)
    }

    override fun getDataSourceName(): String = dataSourceName

    override fun getDataSourceType(): DataSourceType = DataSourceType.MONGODB

    private fun replacePlaceholders(
        doc: Document, 
        parameters: Map<String, String>, 
        parameterTypes: Map<String, String>
    ) {
        replacePlaceholdersRecursive(doc, parameters, parameterTypes)
    }

    private fun replacePlaceholdersRecursive(
        obj: Any, 
        parameters: Map<String, String>, 
        parameterTypes: Map<String, String>
    ): Any {
        return when (obj) {
            is Document -> {
                obj.forEach { (key, value) ->
                    obj[key] = replacePlaceholdersRecursive(value, parameters, parameterTypes)
                }
                obj
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val originalMap = obj as Map<String, Any>
                val map = originalMap.toMutableMap()
                map.forEach { (key, value) ->
                    map[key] = replacePlaceholdersRecursive(value, parameters, parameterTypes)
                }
                map
            }
            is List<*> -> {
                obj.map { replacePlaceholdersRecursive(it!!, parameters, parameterTypes) }
            }
            is String -> {
                // Check if this string is a placeholder like ?tradeDate, ?regionId, ?region
                var workingString: String = obj
                var finalResult: Any? = null
                
                for (paramName in parameterTypes.keys) {
                    val placeholder = "?$paramName"
                    val currentStr: String = workingString
                    
                    if (currentStr == placeholder) {
                        val paramValue = parameters[paramName]
                            ?: throw IllegalArgumentException("Missing required parameter: $paramName")
                        
                        finalResult = when (parameterTypes[paramName]?.uppercase()) {
                            "DATE" -> {
                                try {
                                    LocalDate.parse(paramValue).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Invalid date format for parameter $paramName: $paramValue", e)
                                }
                            }
                            "INTEGER", "INT" -> {
                                try {
                                    paramValue.toInt()
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Invalid integer format for parameter $paramName: $paramValue", e)
                                }
                            }
                            "STRING", "VARCHAR", "TEXT" -> paramValue
                            else -> throw IllegalArgumentException("Unsupported parameter type: ${parameterTypes[paramName]} for parameter $paramName")
                        }
                        break
                    } else if (currentStr.contains(placeholder)) {
                        // If placeholder is embedded in a string, replace it
                        val paramValue = parameters[paramName]
                            ?: throw IllegalArgumentException("Missing required parameter: $paramName")
                        
                        val convertedValue = when (parameterTypes[paramName]?.uppercase()) {
                            "DATE" -> {
                                try {
                                    LocalDate.parse(paramValue).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Invalid date format for parameter $paramName: $paramValue", e)
                                }
                            }
                            "INTEGER", "INT" -> {
                                try {
                                    paramValue.toInt().toString()
                                } catch (e: Exception) {
                                    throw IllegalArgumentException("Invalid integer format for parameter $paramName: $paramValue", e)
                                }
                            }
                            "STRING", "VARCHAR", "TEXT" -> paramValue
                            else -> throw IllegalArgumentException("Unsupported parameter type: ${parameterTypes[paramName]} for parameter $paramName")
                        }
                        workingString = currentStr.replace(placeholder, convertedValue)
                        finalResult = workingString
                    }
                }
                
                finalResult ?: obj
            }
            else -> obj
        }
    }
}

