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
        tradeDate: LocalDate,
        queryConfig: QueryConfig
    ): Long {
        val query = queryConfig.count as? String
            ?: throw IllegalArgumentException("Couchbase query must be a string")

        // Replace placeholders in query
        val formattedQuery = formatQuery(query, tradeDate)

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

    private fun formatQuery(query: String, tradeDate: LocalDate): String {
        var formatted = query
        
        // Replace $1, $2, etc. with actual values
        // For simplicity, assuming tradeDate is the first parameter
        val dateStr = tradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        formatted = formatted.replace("$1", "'$dateStr'")
        formatted = formatted.replace("$tradeDate", "'$dateStr'")
        
        return formatted
    }
}

