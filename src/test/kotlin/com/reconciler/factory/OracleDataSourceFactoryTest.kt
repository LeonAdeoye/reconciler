package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OracleDataSourceFactoryTest {

    private lateinit var factory: OracleDataSourceFactory

    @BeforeEach
    fun setUp() {
        factory = OracleDataSourceFactory()
    }

    @Test
    fun `should support ORACLE type`() {
        assertTrue(factory.supports(DataSourceType.ORACLE))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(factory.supports(DataSourceType.COUCHBASE))
        assertFalse(factory.supports(DataSourceType.MONGODB))
    }

    @Test
    fun `should throw exception when JDBC URL not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = mapOf(
                "username" to "user",
                "password" to "pass"
            ),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when username not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = mapOf(
                "jdbcUrl" to "jdbc:oracle:thin:@localhost:1521:XE",
                "password" to "pass"
            ),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when password not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = mapOf(
                "jdbcUrl" to "jdbc:oracle:thin:@localhost:1521:XE",
                "username" to "user"
            ),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }
}

