package com.reconciler.datasource

import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

class OracleReconciliationDataSourceTest {

    private lateinit var dataSource: DataSource
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var oracleDataSource: OracleReconciliationDataSource

    @BeforeEach
    fun setUp() {
        dataSource = mock()
        jdbcTemplate = mock()
        oracleDataSource = OracleReconciliationDataSource("oracle-ds", dataSource)
        // Use reflection to set jdbcTemplate for testing
        val field = OracleReconciliationDataSource::class.java.getDeclaredField("jdbcTemplate")
        field.isAccessible = true
        field.set(oracleDataSource, jdbcTemplate)
    }

    @Test
    fun `should get count successfully`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) as count FROM orders WHERE trade_date = :tradeDate",
            parameters = null
        )

        whenever(jdbcTemplate.queryForObject(
            any<String>(),
            any<org.springframework.jdbc.core.RowMapper<Long>>(),
            anyVararg()
        )).thenReturn(1500L)

        val result = oracleDataSource.getCount(EntityType.ORDER, tradeDate, queryConfig)

        assertEquals(1500L, result)
        verify(jdbcTemplate).queryForObject(
            any<String>(),
            any<org.springframework.jdbc.core.RowMapper<Long>>(),
            anyVararg()
        )
    }

    @Test
    fun `should return zero when query returns null`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) as count FROM orders WHERE trade_date = :tradeDate",
            parameters = null
        )

        whenever(jdbcTemplate.queryForObject(
            any<String>(),
            any<org.springframework.jdbc.core.RowMapper<Long>>(),
            anyVararg()
        )).thenReturn(null)

        val result = oracleDataSource.getCount(EntityType.ORDER, tradeDate, queryConfig)

        assertEquals(0L, result)
    }

    @Test
    fun `should throw exception when query is not a string`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = mapOf("field" to "value"), // Not a string
            parameters = null
        )

        assertThrows<IllegalArgumentException> {
            oracleDataSource.getCount(EntityType.ORDER, tradeDate, queryConfig)
        }
    }

    @Test
    fun `should return datasource name`() {
        assertEquals("oracle-ds", oracleDataSource.getDataSourceName())
    }

    @Test
    fun `should return ORACLE type`() {
        assertEquals(DataSourceType.ORACLE, oracleDataSource.getDataSourceType())
    }
}

