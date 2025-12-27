package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.datasource.ReconciliationDataSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class DataSourceFactoryRegistryTest {

    private lateinit var couchbaseFactory: DataSourceFactory
    private lateinit var mongoFactory: DataSourceFactory
    private lateinit var oracleFactory: DataSourceFactory
    private lateinit var registry: DataSourceFactoryRegistry
    private lateinit var mockDataSource: ReconciliationDataSource

    @BeforeEach
    fun setUp() {
        couchbaseFactory = mock()
        mongoFactory = mock()
        oracleFactory = mock()
        mockDataSource = mock()

        whenever(couchbaseFactory.supports(DataSourceType.COUCHBASE)).thenReturn(true)
        whenever(mongoFactory.supports(DataSourceType.MONGODB)).thenReturn(true)
        whenever(oracleFactory.supports(DataSourceType.ORACLE)).thenReturn(true)

        registry = DataSourceFactoryRegistry(listOf(couchbaseFactory, mongoFactory, oracleFactory))
        registry.init()
    }

    @Test
    fun `should create datasource using correct factory`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        whenever(couchbaseFactory.createDataSource(config)).thenReturn(mockDataSource)
        whenever(mockDataSource.getDataSourceName()).thenReturn("cb-ds")

        val result = registry.createDataSource(config)

        assertNotNull(result)
        verify(couchbaseFactory).createDataSource(config)
        verify(mongoFactory, never()).createDataSource(any())
        verify(oracleFactory, never()).createDataSource(any())
    }

    @Test
    fun `should cache datasource after first creation`() {
        val config = DataSourceConfig(
            type = DataSourceType.MONGODB,
            name = "mongo-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        whenever(mongoFactory.createDataSource(config)).thenReturn(mockDataSource)
        whenever(mockDataSource.getDataSourceName()).thenReturn("mongo-ds")

        val result1 = registry.createDataSource(config)
        val result2 = registry.createDataSource(config)

        assertSame(result1, result2)
        verify(mongoFactory, times(1)).createDataSource(config)
    }

    @Test
    fun `should throw exception when factory not found`() {
        val config = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "cb-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.QUOTE),
            queries = null
        )

        val emptyRegistry = DataSourceFactoryRegistry(emptyList())
        emptyRegistry.init()

        assertThrows<IllegalArgumentException> {
            emptyRegistry.createDataSource(config)
        }
    }

    @Test
    fun `should clear cache`() {
        val config = DataSourceConfig(
            type = DataSourceType.ORACLE,
            name = "oracle-ds",
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.ORDER),
            queries = null
        )

        whenever(oracleFactory.createDataSource(config)).thenReturn(mockDataSource)
        whenever(mockDataSource.getDataSourceName()).thenReturn("oracle-ds")

        registry.createDataSource(config)
        registry.clearCache()
        registry.createDataSource(config)

        verify(oracleFactory, times(2)).createDataSource(config)
    }
}

