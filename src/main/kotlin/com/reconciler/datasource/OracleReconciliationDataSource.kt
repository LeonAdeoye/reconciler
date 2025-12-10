package com.reconciler.datasource

import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

class OracleReconciliationDataSource(
    private val dataSourceName: String,
    private val dataSource: DataSource
) : ReconciliationDataSource {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    override fun getCount(
        entityType: EntityType,
        tradeDate: LocalDate,
        queryConfig: QueryConfig
    ): Long {
        val query = queryConfig.count as? String
            ?: throw IllegalArgumentException("Oracle query must be a string")

        // Convert named parameters to positional parameters
        val (formattedQuery, params) = formatQuery(query, tradeDate)

        return jdbcTemplate.queryForObject(
            formattedQuery,
            RowMapper<Long> { rs: ResultSet, _: Int -> rs.getLong(1) },
            *params.toTypedArray()
        ) ?: 0L
    }

    override fun getDataSourceName(): String = dataSourceName

    override fun getDataSourceType(): DataSourceType = DataSourceType.ORACLE

    private fun formatQuery(query: String, tradeDate: LocalDate): Pair<String, List<Any>> {
        var formatted = query
        val params = mutableListOf<Any>()
        
        // Replace :tradeDate or :trade_date with ?
        formatted = formatted.replace(Regex(":tradeDate|:trade_date"), "?")
        params.add(java.sql.Date.valueOf(tradeDate))
        
        return Pair(formatted, params)
    }
}

