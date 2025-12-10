package com.reconciler.config.models

data class ReconciliationRule(
    val name: String,
    val sourceSystemA: String,
    val sourceSystemB: String,
    val entityType: EntityType,
    val tradeDateField: String = "tradeDate"
)

