package com.reconciler.controller.dto

import com.reconciler.config.models.EntityType
import java.time.LocalDate

data class AdHocReconciliationRequest(
    val ruleName: String = "",
    val sourceSystemA: String,
    val dataSourceA: String,
    val sourceSystemB: String,
    val dataSourceB: String,
    val entityType: EntityType,
    val tradeDate: LocalDate,
    val tradeDateField: String = "tradeDate",
    val persistRule: Boolean = false
)

