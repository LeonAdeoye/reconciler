package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MongoDataSourceFactoryTest {

    private lateinit var factory: MongoDataSourceFactory

    @BeforeEach
    fun setUp() {
        factory = MongoDataSourceFactory()
    }

    @Test
    fun `should support MONGODB type`() {
        assertTrue(factory.supports(DataSourceType.MONGODB))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(factory.supports(DataSourceType.COUCHBASE))
        assertFalse(factory.supports(DataSourceType.ORACLE))
    }

    @Test
    fun `should throw exception when URI not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.MONGODB,
            name = "mongo-ds",
            connectionConfig = mapOf(
                "database" to "test-db"
            ),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when database not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.MONGODB,
            name = "mongo-ds",
            connectionConfig = mapOf(
                "uri" to "mongodb://localhost:27017"
            ),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }
}

