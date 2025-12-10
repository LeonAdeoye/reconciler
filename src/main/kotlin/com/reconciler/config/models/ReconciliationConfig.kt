package com.reconciler.config.models

data class ReconciliationConfig(
    val sourceSystems: List<SourceSystemConfig>,
    val reconciliationRules: List<ReconciliationRule>
)

