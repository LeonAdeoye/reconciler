package com.reconciler.datasource

import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

class OracleReconciliationDataSource(
    private val dataSourceName: String,
    private val dataSource: DataSource
) : ReconciliationDataSource {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    override fun getCount(
        entityType: EntityType,
        parameters: Map<String, String>,
        queryConfig: QueryConfig
    ): Long {
        val query = queryConfig.count as? String
            ?: throw IllegalArgumentException("Oracle query must be a string")

        // Convert named parameters to positional parameters
        val (formattedQuery, params) = formatQuery(query, parameters, queryConfig.parameters ?: emptyMap())

        return jdbcTemplate.queryForObject(
            formattedQuery,
            RowMapper<Long> { rs: ResultSet, _: Int -> rs.getLong(1) },
            *params.toTypedArray()
        ) ?: 0L
    }

    override fun getDataSourceName(): String = dataSourceName

    override fun getDataSourceType(): DataSourceType = DataSourceType.ORACLE

    private fun formatQuery(
        query: String, 
        parameters: Map<String, String>, 
        parameterTypes: Map<String, String>
    ): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val paramValueCache = mutableMapOf<String, Any>()
        
        // Pre-convert all parameter values
        parameterTypes.forEach { (paramName, paramType) ->
            val paramValue = parameters[paramName]
                ?: throw IllegalArgumentException("Missing required parameter: $paramName")
            
            val convertedValue = when (paramType.uppercase()) {
                "DATE" -> {
                    try {
                        java.sql.Date.valueOf(LocalDate.parse(paramValue))
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
                else -> throw IllegalArgumentException("Unsupported parameter type: $paramType for parameter $paramName")
            }
            
            paramValueCache[paramName] = convertedValue
        }
        
        // Process query sequentially to replace placeholders in order
        var formatted = query
        val placeholderPattern = Regex(":([a-zA-Z][a-zA-Z0-9]*)")
        
        placeholderPattern.findAll(query).forEach { matchResult ->
            val placeholder = matchResult.value // e.g., ":tradeDate"
            val paramName = matchResult.groupValues[1] // e.g., "tradeDate"
            
            // Match parameter by exact camelCase name
            if (parameterTypes.containsKey(paramName) && paramValueCache.containsKey(paramName)) {
                formatted = formatted.replaceFirst(placeholder, "?")
                params.add(paramValueCache[paramName]!!)
            }
        }
        
        return Pair(formatted, params)
    }
}

