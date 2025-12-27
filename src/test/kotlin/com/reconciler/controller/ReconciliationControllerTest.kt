package com.reconciler.controller

import com.reconciler.config.ReconciliationConfigLoader
import com.reconciler.config.models.*
import com.reconciler.controller.dto.*
import com.reconciler.reconciliation.ReconciliationResult
import com.reconciler.reconciliation.ReconciliationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.time.LocalDate

class ReconciliationControllerTest {

    private lateinit var reconciliationService: ReconciliationService
    private lateinit var configLoader: ReconciliationConfigLoader
    private lateinit var controller: ReconciliationController

    @BeforeEach
    fun setUp() {
        reconciliationService = mock()
        configLoader = mock()
        controller = ReconciliationController(reconciliationService, configLoader)
    }

    @Test
    fun `should get all rules`() {
        val rules = listOf(
            ReconciliationRule("rule-1", "system-a", "system-b", EntityType.QUOTE),
            ReconciliationRule("rule-2", "system-a", "system-b", EntityType.ORDER)
        )

        whenever(configLoader.getAllRules()).thenReturn(rules)

        val result = controller.getRules()

        assertEquals(2, result.size)
        assertEquals("rule-1", result[0].name)
        assertEquals("rule-2", result[1].name)
    }

    @Test
    fun `should get all systems`() {
        val systems = listOf(
            createTestSystem("system-a"),
            createTestSystem("system-b")
        )

        whenever(configLoader.getAllSourceSystems()).thenReturn(systems)

        val result = controller.getSystems()

        assertEquals(2, result.size)
        assertTrue(result.containsKey("system-a"))
        assertTrue(result.containsKey("system-b"))
    }

    @Test
    fun `should execute reconciliation`() {
        val ruleName = "test-rule"
        val tradeDate = LocalDate.of(2024, 1, 15)
        val request = ExecuteReconciliationRequest(tradeDate)
        val reconciliationResult = createTestResult(ruleName, true, 0L)

        whenever(reconciliationService.executeReconciliation(ruleName, tradeDate))
            .thenReturn(reconciliationResult)

        val response = controller.executeReconciliation(ruleName, request)

        assertNotNull(response)
        assertEquals(ruleName, response.result.ruleName)
        assertFalse(response.ruleCreated)
        assertEquals(ruleName, response.ruleName)
    }

    @Test
    fun `should execute ad-hoc reconciliation`() {
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
        val reconciliationResult = createTestResult("adhoc-rule", true, 0L)

        whenever(reconciliationService.executeAdHocReconciliation(request))
            .thenReturn(reconciliationResult)

        val response = controller.executeAdHocReconciliation(request)

        assertNotNull(response)
        assertFalse(response.ruleCreated)
        assertNull(response.ruleName)
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
        val reconciliationResult = createTestResult("adhoc-rule", true, 0L)

        whenever(reconciliationService.executeAdHocReconciliation(request))
            .thenReturn(reconciliationResult)

        val response = controller.executeAdHocReconciliation(request)

        assertTrue(response.ruleCreated)
        assertEquals("adhoc-rule", response.ruleName)
    }

    @Test
    fun `should create rule successfully`() {
        val rule = ReconciliationRule(
            name = "new-rule",
            sourceSystemA = "system-a",
            sourceSystemB = "system-b",
            entityType = EntityType.QUOTE
        )

        val systemA = createTestSystem("system-a")
        val systemB = createTestSystem("system-b")

        whenever(configLoader.getSourceSystem("system-a")).thenReturn(systemA)
        whenever(configLoader.getSourceSystem("system-b")).thenReturn(systemB)

        val response = controller.createRule(rule)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(rule, response.body)
        verify(configLoader).addRule(rule)
    }

    @Test
    fun `should throw exception when creating rule with invalid system`() {
        val rule = ReconciliationRule(
            name = "new-rule",
            sourceSystemA = "non-existent",
            sourceSystemB = "system-b",
            entityType = EntityType.QUOTE
        )

        whenever(configLoader.getSourceSystem("non-existent")).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            controller.createRule(rule)
        }
    }

    @Test
    fun `should delete rule successfully`() {
        whenever(configLoader.removeRule("rule-1")).thenReturn(true)

        val response = controller.deleteRule("rule-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(configLoader).removeRule("rule-1")
    }

    @Test
    fun `should return not found when deleting non-existent rule`() {
        whenever(configLoader.removeRule("non-existent")).thenReturn(false)

        val response = controller.deleteRule("non-existent")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should get available reconciliations`() {
        val systemA = createTestSystem("system-a", listOf(EntityType.QUOTE, EntityType.ORDER))
        val systemB = createTestSystem("system-b", listOf(EntityType.QUOTE, EntityType.ORDER))

        whenever(configLoader.getAllSourceSystems()).thenReturn(listOf(systemA, systemB))

        val result = controller.getAvailableReconciliations()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        val reconciliation = result[0]
        assertEquals("system-a", reconciliation.sourceSystemA)
        assertEquals("system-b", reconciliation.sourceSystemB)
        assertTrue(reconciliation.availableEntityTypes.contains("QUOTE"))
        assertTrue(reconciliation.availableEntityTypes.contains("ORDER"))
    }

    @Test
    fun `should return empty list when no common entity types`() {
        val systemA = createTestSystem("system-a", listOf(EntityType.QUOTE))
        val systemB = createTestSystem("system-b", listOf(EntityType.ORDER))

        whenever(configLoader.getAllSourceSystems()).thenReturn(listOf(systemA, systemB))

        val result = controller.getAvailableReconciliations()

        assertTrue(result.isEmpty())
    }

    private fun createTestSystem(name: String, entityTypes: List<EntityType> = listOf(EntityType.QUOTE)): SourceSystemConfig {
        val dataSource = DataSourceConfig(
            type = DataSourceType.COUCHBASE,
            name = "ds-$name",
            connectionConfig = emptyMap(),
            entityTypes = entityTypes,
            queries = null
        )
        return SourceSystemConfig(name = name, dataSources = listOf(dataSource))
    }

    private fun createTestResult(ruleName: String, match: Boolean, difference: Long): ReconciliationResult {
        return ReconciliationResult(
            ruleName = ruleName,
            sourceSystemA = "system-a",
            sourceSystemB = "system-b",
            entityType = EntityType.QUOTE,
            tradeDate = LocalDate.of(2024, 1, 15),
            countA = 1000L,
            countB = 1000L,
            match = match,
            difference = difference,
            timestamp = Instant.now(),
            dataSourceA = "ds-a",
            dataSourceB = "ds-b"
        )
    }
}

