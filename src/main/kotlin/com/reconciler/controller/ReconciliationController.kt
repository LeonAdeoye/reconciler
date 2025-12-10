package com.reconciler.controller

import com.reconciler.config.ReconciliationConfigLoader
import com.reconciler.config.models.ReconciliationRule
import com.reconciler.controller.dto.*
import com.reconciler.reconciliation.ReconciliationResult
import com.reconciler.reconciliation.ReconciliationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/reconciliation")
class ReconciliationController(
    private val reconciliationService: ReconciliationService,
    private val configLoader: ReconciliationConfigLoader
) {

    @GetMapping("/rules")
    fun getRules(): List<ReconciliationRule> {
        return configLoader.getAllRules()
    }

    @GetMapping("/systems")
    fun getSystems(): Map<String, SystemInfoResponse> {
        return configLoader.getAllSourceSystems()
            .associateBy({ it.name }, { it.toSystemInfoResponse() })
    }

    @PostMapping("/execute/{ruleName}")
    fun executeReconciliation(
        @PathVariable ruleName: String,
        @RequestBody @Valid request: ExecuteReconciliationRequest
    ): ReconciliationResponse {
        val result = reconciliationService.executeReconciliation(
            ruleName = ruleName,
            tradeDate = request.tradeDate
        )
        return ReconciliationResponse(
            result = result,
            ruleCreated = false,
            ruleName = ruleName
        )
    }

    @PostMapping("/execute-adhoc")
    fun executeAdHocReconciliation(
        @RequestBody @Valid request: AdHocReconciliationRequest
    ): ReconciliationResponse {
        val result = reconciliationService.executeAdHocReconciliation(request)
        return ReconciliationResponse(
            result = result,
            ruleCreated = request.persistRule,
            ruleName = if (request.persistRule) result.ruleName else null
        )
    }

    @PostMapping("/rules")
    fun createRule(@RequestBody @Valid rule: ReconciliationRule): ResponseEntity<ReconciliationRule> {
        validateRule(rule)
        configLoader.addRule(rule)
        return ResponseEntity.status(HttpStatus.CREATED).body(rule)
    }

    @DeleteMapping("/rules/{ruleName}")
    fun deleteRule(@PathVariable ruleName: String): ResponseEntity<Void> {
        val deleted = configLoader.removeRule(ruleName)
        return if (deleted) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/available-reconciliations")
    fun getAvailableReconciliations(): List<AvailableReconciliation> {
        val systems = configLoader.getAllSourceSystems()
        val reconciliations = mutableListOf<AvailableReconciliation>()

        systems.forEach { systemA ->
            systems.forEach { systemB ->
                if (systemA.name != systemB.name) {
                    val commonEntityTypes = findCommonEntityTypes(systemA, systemB)
                    if (commonEntityTypes.isNotEmpty()) {
                        reconciliations.add(
                            AvailableReconciliation(
                                sourceSystemA = systemA.name,
                                sourceSystemB = systemB.name,
                                availableEntityTypes = commonEntityTypes,
                                dataSourcePairs = findDataSourcePairs(systemA, systemB, commonEntityTypes)
                            )
                        )
                    }
                }
            }
        }

        return reconciliations
    }

    private fun validateRule(rule: ReconciliationRule) {
        val systemA = configLoader.getSourceSystem(rule.sourceSystemA)
            ?: throw IllegalArgumentException("Source system A not found: ${rule.sourceSystemA}")

        val systemB = configLoader.getSourceSystem(rule.sourceSystemB)
            ?: throw IllegalArgumentException("Source system B not found: ${rule.sourceSystemB}")

        val hasDataSourceA = systemA.dataSources.any {
            it.entityTypes.contains(rule.entityType)
        }
        val hasDataSourceB = systemB.dataSources.any {
            it.entityTypes.contains(rule.entityType)
        }

        if (!hasDataSourceA) {
            throw IllegalArgumentException(
                "System ${rule.sourceSystemA} has no data source supporting ${rule.entityType}"
            )
        }

        if (!hasDataSourceB) {
            throw IllegalArgumentException(
                "System ${rule.sourceSystemB} has no data source supporting ${rule.entityType}"
            )
        }
    }

    private fun findCommonEntityTypes(
        systemA: com.reconciler.config.models.SourceSystemConfig,
        systemB: com.reconciler.config.models.SourceSystemConfig
    ): List<String> {
        val entityTypesA = systemA.dataSources.flatMap { it.entityTypes }.toSet()
        val entityTypesB = systemB.dataSources.flatMap { it.entityTypes }.toSet()
        return entityTypesA.intersect(entityTypesB).map { it.name }
    }

    private fun findDataSourcePairs(
        systemA: com.reconciler.config.models.SourceSystemConfig,
        systemB: com.reconciler.config.models.SourceSystemConfig,
        entityTypes: List<String>
    ): Map<String, List<DataSourcePair>> {
        val pairs = mutableMapOf<String, MutableList<DataSourcePair>>()

        entityTypes.forEach { entityTypeName ->
            val entityType = com.reconciler.config.models.EntityType.valueOf(entityTypeName)
            val sourcesA = systemA.dataSources.filter { it.entityTypes.contains(entityType) }
            val sourcesB = systemB.dataSources.filter { it.entityTypes.contains(entityType) }

            val pairsForEntity = mutableListOf<DataSourcePair>()
            sourcesA.forEach { dsA ->
                sourcesB.forEach { dsB ->
                    pairsForEntity.add(
                        DataSourcePair(
                            dataSourceA = dsA.name,
                            dataSourceB = dsB.name
                        )
                    )
                }
            }
            pairs[entityTypeName] = pairsForEntity
        }

        return pairs
    }
}

data class AvailableReconciliation(
    val sourceSystemA: String,
    val sourceSystemB: String,
    val availableEntityTypes: List<String>,
    val dataSourcePairs: Map<String, List<DataSourcePair>>
)

data class DataSourcePair(
    val dataSourceA: String,
    val dataSourceB: String
)

