package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CouchbaseDataSourceFactoryTest {

    private lateinit var factory: CouchbaseDataSourceFactory

    @BeforeEach
    fun setUp() {
        factory = CouchbaseDataSourceFactory()
    }

    @Test
    fun `should support COUCHBASE type`() {
        assertTrue(factory.supports(DataSourceType.COUCHBASE))
    }

    @Test
    fun `should not support other types`() {
        assertFalse(factory.supports(DataSourceType.MONGODB))
        assertFalse(factory.supports(DataSourceType.ORACLE))
    }

    @Test
    fun `should throw exception when hosts not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = mapOf(
                "bucket" to "test-bucket",
                "username" to "user",
                "password" to "pass"
            ),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when bucket not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = mapOf(
                "hosts" to listOf("localhost:8091"),
                "username" to "user",
                "password" to "pass"
            ),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when username not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = mapOf(
                "hosts" to listOf("localhost:8091"),
                "bucket" to "test-bucket",
                "password" to "pass"
            ),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }

    @Test
    fun `should throw exception when password not specified`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = mapOf(
                "hosts" to listOf("localhost:8091"),
                "bucket" to "test-bucket",
                "username" to "user"
            ),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        assertThrows<IllegalArgumentException> {
            factory.createDataSource(config)
        }
    }
}

