package com.reconciler.reconciliation

import com.reconciler.config.ReconciliationConfigLoader
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.ReconciliationRule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate

class ReconciliationExecutorTest {

    private lateinit var reconciliationService: ReconciliationService
    private lateinit var configLoader: ReconciliationConfigLoader
    private lateinit var executor: ReconciliationExecutor

    @BeforeEach
    fun setUp() {
        reconciliationService = mock()
        configLoader = mock()
        executor = ReconciliationExecutor(reconciliationService, configLoader)
    }

    @Test
    fun `should execute all rules successfully`() {
        val tradeDate = LocalDate.of(2024, 1, 15)
        val rules = listOf(
            ReconciliationRule("rule-1", "system-a", "system-b", EntityType.QUOTE),
            ReconciliationRule("rule-2", "system-a", "system-b", EntityType.ORDER)
        )

        val result1 = createTestResult("rule-1", true, 0L)
        val result2 = createTestResult("rule-2", true, 0L)

        whenever(configLoader.getAllRules()).thenReturn(rules)
        whenever(reconciliationService.executeReconciliation("rule-1", tradeDate)).thenReturn(result1)
        whenever(reconciliationService.executeReconciliation("rule-2", tradeDate)).thenReturn(result2)

        val results = executor.executeAllRules(tradeDate)

        assertEquals(2, results.size)
        assertEquals("rule-1", results[0].ruleName)
        assertEquals("rule-2", results[1].ruleName)
        verify(reconciliationService).executeReconciliation("rule-1", tradeDate)
        verify(reconciliationService).executeReconciliation("rule-2", tradeDate)
    }

    @Test
    fun `should continue executing when one rule fails`() {
        val tradeDate = LocalDate.of(2024, 1, 15)
        val rules = listOf(
            ReconciliationRule("rule-1", "system-a", "system-b", EntityType.QUOTE),
            ReconciliationRule("rule-2", "system-a", "system-b", EntityType.ORDER)
        )

        val result1 = createTestResult("rule-1", true, 0L)

        whenever(configLoader.getAllRules()).thenReturn(rules)
        whenever(reconciliationService.executeReconciliation("rule-1", tradeDate)).thenReturn(result1)
        whenever(reconciliationService.executeReconciliation("rule-2", tradeDate))
            .thenThrow(RuntimeException("Error executing rule"))

        val results = executor.executeAllRules(tradeDate)

        assertEquals(1, results.size)
        assertEquals("rule-1", results[0].ruleName)
    }

    @Test
    fun `should return empty list when no rules configured`() {
        val tradeDate = LocalDate.of(2024, 1, 15)

        whenever(configLoader.getAllRules()).thenReturn(emptyList())

        val results = executor.executeAllRules(tradeDate)

        assertTrue(results.isEmpty())
        verify(reconciliationService, never()).executeReconciliation(any(), any())
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

