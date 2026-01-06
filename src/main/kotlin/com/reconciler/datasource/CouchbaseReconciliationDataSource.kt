package com.reconciler.datasource

import com.couchbase.client.java.Bucket
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.query.QueryOptions
import com.couchbase.client.java.query.QueryResult
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CouchbaseReconciliationDataSource(
    private val dataSourceName: String,
    private val cluster: Cluster,
    private val bucket: Bucket
) : ReconciliationDataSource {

    override fun getCount(
        entityType: EntityType,
        parameters: Map<String, String>,
        queryConfig: QueryConfig
    ): Long {
        val query = queryConfig.count as? String
            ?: throw IllegalArgumentException("Couchbase query must be a string")

        // Replace placeholders in query
        val formattedQuery = formatQuery(query, parameters, queryConfig.parameters ?: emptyMap())

        val result: QueryResult = cluster.query(formattedQuery, QueryOptions.queryOptions())
        
        val rows = result.rowsAsObject()
        if (rows.isEmpty()) {
            return 0L
        }

        val row = rows[0]
        val countValue = row.get("count")
        
        return when (countValue) {
            is Number -> countValue.toLong()
            is String -> countValue.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    override fun getDataSourceName(): String = dataSourceName

    override fun getDataSourceType(): DataSourceType = DataSourceType.COUCHBASE

    private fun formatQuery(
        query: String, 
        parameters: Map<String, String>, 
        parameterTypes: Map<String, String>
    ): String {
        var formatted = query
        
        // Process each parameter defined in the config
        parameterTypes.forEach { (paramName, paramType) ->
            val paramValue = parameters[paramName]
                ?: throw IllegalArgumentException("Missing required parameter: $paramName")
            
            val convertedValue = when (paramType.uppercase()) {
                "DATE" -> {
                    try {
                        val date = LocalDate.parse(paramValue)
                        "'${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}'"
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
                "STRING", "VARCHAR", "TEXT" -> "'$paramValue'"
                else -> throw IllegalArgumentException("Unsupported parameter type: $paramType for parameter $paramName")
            }
            
            // Replace placeholders: $paramName, $1, $2, etc., and ?paramName
            formatted = formatted.replace("$$paramName", convertedValue)
            formatted = formatted.replace("?$paramName", convertedValue)
            
            // Also support positional parameters $1, $2, etc. if they match the order
            // This is a simple approach - for more complex cases, you might need parameter order tracking
            val paramIndex = parameterTypes.keys.indexOf(paramName) + 1
            formatted = formatted.replace("$$paramIndex", convertedValue)
        }
        
        return formatted
    }
}

