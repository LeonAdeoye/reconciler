package com.reconciler.reconciliation

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReconciliationLogger(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ReconciliationLogger::class.java)

    fun logResult(result: ReconciliationResult) {
        val logData = mapOf(
            "event" to "reconciliation_result",
            "ruleName" to result.ruleName,
            "sourceSystemA" to result.sourceSystemA,
            "sourceSystemB" to result.sourceSystemB,
            "entityType" to result.entityType.name,
            "tradeDate" to result.tradeDate.toString(),
            "countA" to result.countA,
            "countB" to result.countB,
            "match" to result.match,
            "difference" to result.difference,
            "timestamp" to result.timestamp.toString(),
            "dataSourceA" to result.dataSourceA,
            "dataSourceB" to result.dataSourceB
        )

        val jsonLog = objectMapper.writeValueAsString(logData)
        logger.info(jsonLog)
    }
}

