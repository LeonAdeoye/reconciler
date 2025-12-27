package com.reconciler.config

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QueryProviderTest {

    private lateinit var queryProvider: QueryProvider

    @BeforeEach
    fun setUp() {
        queryProvider = QueryProvider()
    }

    @Test
    fun `should get count query successfully`() {
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) FROM table",
            parameters = mapOf("tradeDate" to "DATE")
        )

        val dataSourceConfig = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.ORDER),
            queries = mapOf("ORDER" to queryConfig)
        )

        val result = queryProvider.getCountQuery(dataSourceConfig, EntityType.ORDER)

        assertNotNull(result)
        assertEquals("SELECT COUNT(*) FROM table", result.count)
    }

    @Test
    fun `should throw exception when queries not defined`() {
        val dataSourceConfig = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            queryProvider.getCountQuery(dataSourceConfig, EntityType.ORDER)
        }
    }

    @Test
    fun `should throw exception when entity type query not found`() {
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) FROM table",
            parameters = null
        )

        val dataSourceConfig = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.ORDER),
            queries = mapOf("QUOTE" to queryConfig) // ORDER query missing
        )

        assertThrows<IllegalArgumentException> {
            queryProvider.getCountQuery(dataSourceConfig, EntityType.ORDER)
        }
    }

    @Test
    fun `should get query for different entity types`() {
        val quoteQuery = QueryConfig(count = "SELECT COUNT(*) FROM quotes", parameters = null)
        val orderQuery = QueryConfig(count = "SELECT COUNT(*) FROM orders", parameters = null)

        val dataSourceConfig = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.QUOTE, EntityType.ORDER),
            queries = mapOf(
                "QUOTE" to quoteQuery,
                "ORDER" to orderQuery
            )
        )

        val quoteResult = queryProvider.getCountQuery(dataSourceConfig, EntityType.QUOTE)
        val orderResult = queryProvider.getCountQuery(dataSourceConfig, EntityType.ORDER)

        assertEquals("SELECT COUNT(*) FROM quotes", quoteResult.count)
        assertEquals("SELECT COUNT(*) FROM orders", orderResult.count)
    }
}

