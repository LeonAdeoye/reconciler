package com.reconciler.reconciliation

import com.reconciler.config.ReconciliationConfigLoader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReconciliationExecutor(
    private val reconciliationService: ReconciliationService,
    private val configLoader: ReconciliationConfigLoader
) {
    private val logger = LoggerFactory.getLogger(ReconciliationExecutor::class.java)

    fun executeAllRules(tradeDate: LocalDate): List<ReconciliationResult> {
        val rules = configLoader.getAllRules()
        logger.info("Executing ${rules.size} reconciliation rules for trade date: $tradeDate")

        val results = mutableListOf<ReconciliationResult>()

        rules.forEach { rule ->
            try {
                logger.info("Executing rule: ${rule.name}")
                val result = reconciliationService.executeReconciliation(rule.name, tradeDate)
                results.add(result)
                logger.info("Rule ${rule.name} completed: match=${result.match}, difference=${result.difference}")
            } catch (e: Exception) {
                logger.error("Error executing rule ${rule.name}", e)
                // Continue with other rules
            }
        }

        return results
    }
}

