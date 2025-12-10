package com.reconciler.reconciliation

import com.reconciler.config.models.EntityType
import java.time.Instant
import java.time.LocalDate

data class ReconciliationResult(
    val ruleName: String,
    val sourceSystemA: String,
    val sourceSystemB: String,
    val entityType: EntityType,
    val tradeDate: LocalDate,
    val countA: Long,
    val countB: Long,
    val match: Boolean,
    val difference: Long,
    val timestamp: Instant,
    val dataSourceA: String,
    val dataSourceB: String
)

