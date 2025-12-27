package com.reconciler.reconciliation

import com.reconciler.config.ReconciliationConfigLoader
import com.reconciler.config.QueryProvider
import com.reconciler.config.models.*
import com.reconciler.controller.dto.AdHocReconciliationRequest
import com.reconciler.datasource.ReconciliationDataSource
import com.reconciler.factory.DataSourceFactoryRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate

class ReconciliationServiceTest {

    private lateinit var configLoader: ReconciliationConfigLoader
    private lateinit var factoryRegistry: DataSourceFactoryRegistry
    private lateinit var queryProvider: QueryProvider
    private lateinit var logger: ReconciliationLogger
    private lateinit var service: ReconciliationService
    private lateinit var dataSourceA: ReconciliationDataSource
    private lateinit var dataSourceB: ReconciliationDataSource

    @BeforeEach
    fun setUp() {
        configLoader = mock()
        factoryRegistry = mock()
        queryProvider = mock()
        logger = mock()
        dataSourceA = mock()
        dataSourceB = mock()

        service = ReconciliationService(configLoader, factoryRegistry, queryProvider, logger)
    }

    @Test
    fun `should execute reconciliation successfully with matching counts`() {
        val ruleName = "test-rule"
        val tradeDate = LocalDate.of(2024, 1, 15)
        val rule = createTestRule(ruleName)
        val systemA = createTestSystem("system-a", "ds-a")
        val systemB = createTestSystem("system-b", "ds-b")
        val queryConfig = QueryConfig(count = "SELECT COUNT(*) FROM table", parameters = null)

        whenever(configLoader.getRule(ruleName)).thenReturn(rule)
        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(systemB)
        whenever(factoryRegistry.createDataSource(any())).thenReturn(dataSourceA, dataSourceB)
        whenever(dataSourceA.getDataSourceName()).thenReturn("ds-a")
        whenever(dataSourceB.getDataSourceName()).thenReturn("ds-b")
        whenever(queryProvider.getCountQuery(any(), any())).thenReturn(queryConfig)
        whenever(dataSourceA.getCount(any(), any(), any())).thenReturn(1500L)
        whenever(dataSourceB.getCount(any(), any(), any())).thenReturn(1500L)

        val result = service.executeReconciliation(ruleName, tradeDate)

        assertNotNull(result)
        assertTrue(result.match)
        assertEquals(0L, result.difference)
        assertEquals(1500L, result.countA)
        assertEquals(1500L, result.countB)
        verify(logger).logResult(any())
    }

    @Test
    fun `should execute reconciliation with non-matching counts`() {
        val ruleName = "test-rule"
        val tradeDate = LocalDate.of(2024, 1, 15)
        val rule = createTestRule(ruleName)
        val systemA = createTestSystem("system-a", "ds-a")
        val systemB = createTestSystem("system-b", "ds-b")
        val queryConfig = QueryConfig(count = "SELECT COUNT(*) FROM table", parameters = null)

        whenever(configLoader.getRule(ruleName)).thenReturn(rule)
        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(systemB)
        whenever(factoryRegistry.createDataSource(any())).thenReturn(dataSourceA, dataSourceB)
        whenever(dataSourceA.getDataSourceName()).thenReturn("ds-a")
        whenever(dataSourceB.getDataSourceName()).thenReturn("ds-b")
        whenever(queryProvider.getCountQuery(any(), any())).thenReturn(queryConfig)
        whenever(dataSourceA.getCount(any(), any(), any())).thenReturn(1500L)
        whenever(dataSourceB.getCount(any(), any(), any())).thenReturn(1400L)

        val result = service.executeReconciliation(ruleName, tradeDate)

        assertFalse(result.match)
        assertEquals(100L, result.difference)
    }

    @Test
    fun `should throw exception when rule not found`() {
        val ruleName = "non-existent"
        val tradeDate = LocalDate.of(2024, 1, 15)

        whenever(configLoader.getRule(ruleName)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            service.executeReconciliation(ruleName, tradeDate)
        }
    }

    @Test
    fun `should execute ad-hoc reconciliation successfully`() {
        val tradeDate = LocalDate.of(2024, 1, 15)
        val request = AdHocReconciliationRequest(
            ruleName = "adhoc-rule",
            sourceSystemA = "system-a",
            dataSourceA = "ds-a",
            sourceSystemB = "system-b",
            dataSourceB = "ds-b",
            entityType = EntityType.QUOTE,
            tradeDate = tradeDate,
            persistRule = false
        )

        val systemA = createTestSystem("system-a", "ds-a")
        val systemB = createTestSystem("system-b", "ds-b")
        val queryConfig = QueryConfig(count = "SELECT COUNT(*) FROM table", parameters = null)

        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(systemB)
        whenever(factoryRegistry.createDataSource(any())).thenReturn(dataSourceA, dataSourceB)
        whenever(dataSourceA.getDataSourceName()).thenReturn("ds-a")
        whenever(dataSourceB.getDataSourceName()).thenReturn("ds-b")
        whenever(queryProvider.getCountQuery(any(), any())).thenReturn(queryConfig)
        whenever(dataSourceA.getCount(any(), any(), any())).thenReturn(1000L)
        whenever(dataSourceB.getCount(any(), any(), any())).thenReturn(1000L)

        val result = service.executeAdHocReconciliation(request)

        assertNotNull(result)
        assertTrue(result.match)
        verify(configLoader, never()).addRule(any())
    }

    @Test
    fun `should persist rule when persistRule is true`() {
        val tradeDate = LocalDate.of(2024, 1, 15)
        val request = AdHocReconciliationRequest(
            ruleName = "adhoc-rule",
            sourceSystemA = "system-a",
            dataSourceA = "ds-a",
            sourceSystemB = "system-b",
            dataSourceB = "ds-b",
            entityType = EntityType.QUOTE,
            tradeDate = tradeDate,
            persistRule = true
        )

        val systemA = createTestSystem("system-a", "ds-a")
        val systemB = createTestSystem("system-b", "ds-b")
        val queryConfig = QueryConfig(count = "SELECT COUNT(*) FROM table", parameters = null)

        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(systemB)
        whenever(factoryRegistry.createDataSource(any())).thenReturn(dataSourceA, dataSourceB)
        whenever(dataSourceA.getDataSourceName()).thenReturn("ds-a")
        whenever(dataSourceB.getDataSourceName()).thenReturn("ds-b")
        whenever(queryProvider.getCountQuery(any(), any())).thenReturn(queryConfig)
        whenever(dataSourceA.getCount(any(), any(), any())).thenReturn(1000L)
        whenever(dataSourceB.getCount(any(), any(), any())).thenReturn(1000L)

        val result = service.executeAdHocReconciliation(request)

        assertNotNull(result)
        verify(configLoader).addRule(any())
    }

    @Test
    fun `should throw exception when source system A not found in ad-hoc request`() {
        val request = AdHocReconciliationRequest(
            sourceSystemA = "non-existent",
            dataSourceA = "ds-a",
            sourceSystemB = "system-b",
            dataSourceB = "ds-b",
            entityType = EntityType.QUOTE,
            tradeDate = LocalDate.of(2024, 1, 15)
        )

        whenever(configLoader.getSourceSystem("non-existent")).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            service.executeAdHocReconciliation(request)
        }
    }

    @Test
    fun `should throw exception when datasource A not found in ad-hoc request`() {
        val request = AdHocReconciliationRequest(
            sourceSystemA = "system-a",
            dataSourceA = "non-existent",
            sourceSystemB = "system-b",
            dataSourceB = "ds-b",
            entityType = EntityType.QUOTE,
            tradeDate = LocalDate.of(2024, 1, 15)
        )

        val systemA = createTestSystem("system-a", "ds-a")

        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(createTestSystem("system-b", "ds-b"))

        assertThrows<IllegalArgumentException> {
            service.executeAdHocReconciliation(request)
        }
    }

    private fun createTestRule(name: String): ReconciliationRule {
        return ReconciliationRule(
            name = name,
            sourceSystemA = "system-a",
            sourceSystemB = "system-b",
            entityType = EntityType.QUOTE
        )
    }

    private fun createTestSystem(name: String, dataSourceName: String): SourceSystemConfig {
        val dataSource = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = dataSourceName,
            connectionConfig = emptyMap(),
            entityTypes = listOf(EntityType.QUOTE),
            queries = mapOf("QUOTE" to QueryConfig(count = "SELECT COUNT(*) FROM table", parameters = null))
        )
        return SourceSystemConfig(name = name, dataSources = listOf(dataSource))
    }
}

