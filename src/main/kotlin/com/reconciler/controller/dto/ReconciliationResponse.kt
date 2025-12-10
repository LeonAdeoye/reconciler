package com.reconciler.controller.dto

import com.reconciler.reconciliation.ReconciliationResult

data class ReconciliationResponse(
    val result: ReconciliationResult,
    val ruleCreated: Boolean,
    val ruleName: String?
)

