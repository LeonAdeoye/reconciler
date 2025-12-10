package com.reconciler.controller.dto

import java.time.LocalDate

data class ExecuteReconciliationRequest(
    val tradeDate: LocalDate
)

