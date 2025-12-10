package com.reconciler.reconciliation

import com.reconciler.config.ReconciliationConfigLoader
import com.reconciler.config.QueryProvider
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.ReconciliationRule
import com.reconciler.config.models.SourceSystemConfig
import com.reconciler.controller.dto.AdHocReconciliationRequest
import com.reconciler.datasource.ReconciliationDataSource
import com.reconciler.factory.DataSourceFactoryRegistry
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class ReconciliationService(
    private val configLoader: ReconciliationConfigLoader,
    private val dataSourceFactoryRegistry: DataSourceFactoryRegistry,
    private val queryProvider: QueryProvider,
    private val reconciliationLogger: ReconciliationLogger
) {

    fun executeReconciliation(ruleName: String, tradeDate: LocalDate): ReconciliationResult {
        val rule = configLoader.getRule(ruleName)
            ?: throw IllegalArgumentException("Rule not found: $ruleName")
        return executeReconciliation(rule, tradeDate)
    }

    fun executeAdHocReconciliation(request: AdHocReconciliationRequest): ReconciliationResult {
        validateAdHocRequest(request)

        val tempRule = ReconciliationRule(
            name = request.ruleName.ifBlank { "adhoc-${UUID.randomUUID()}" },
            sourceSystemA = request.sourceSystemA,
            sourceSystemB = request.sourceSystemB,
            entityType = request.entityType,
            tradeDateField = request.tradeDateField
        )

        val result = executeReconciliation(
            rule = tempRule,
            tradeDate = request.tradeDate,
            dataSourceA = request.dataSourceA,
            dataSourceB = request.dataSourceB
        )

        if (request.persistRule) {
            configLoader.addRule(tempRule.copy(name = result.ruleName))
        }

        return result
    }

    private fun executeReconciliation(
        rule: ReconciliationRule,
        tradeDate: LocalDate,
        dataSourceA: String? = null,
        dataSourceB: String? = null
    ): ReconciliationResult {
        val systemA = configLoader.getSourceSystem(rule.sourceSystemA)
            ?: throw IllegalArgumentException("Source system not found: ${rule.sourceSystemA}")
        val systemB = configLoader.getSourceSystem(rule.sourceSystemB)
            ?: throw IllegalArgumentException("Source system not found: ${rule.sourceSystemB}")

        val dsA = findDataSource(systemA, rule.entityType, dataSourceA)
        val dsB = findDataSource(systemB, rule.entityType, dataSourceB)

        val queryConfigA = queryProvider.getCountQuery(
            systemA.dataSources.find { it.name == dsA.getDataSourceName() }!!,
            rule.entityType
        )
        val queryConfigB = queryProvider.getCountQuery(
            systemB.dataSources.find { it.name == dsB.getDataSourceName() }!!,
            rule.entityType
        )

        val countA = dsA.getCount(rule.entityType, tradeDate, queryConfigA)
        val countB = dsB.getCount(rule.entityType, tradeDate, queryConfigB)

        val result = ReconciliationResult(
            ruleName = rule.name,
            sourceSystemA = rule.sourceSystemA,
            sourceSystemB = rule.sourceSystemB,
            entityType = rule.entityType,
            tradeDate = tradeDate,
            countA = countA,
            countB = countB,
            match = countA == countB,
            difference = Math.abs(countA - countB),
            timestamp = Instant.now(),
            dataSourceA = dsA.getDataSourceName(),
            dataSourceB = dsB.getDataSourceName()
        )

        reconciliationLogger.logResult(result)

        return result
    }

    private fun findDataSource(
        system: SourceSystemConfig,
        entityType: EntityType,
        preferredName: String?
    ): ReconciliationDataSource {
        val matchingSources = system.dataSources.filter {
            it.entityTypes.contains(entityType)
        }

        val dataSourceConfig = if (preferredName != null) {
            matchingSources.find { it.name == preferredName }
                ?: throw IllegalArgumentException(
                    "DataSource '$preferredName' not found or doesn't support $entityType"
                )
        } else {
            matchingSources.firstOrNull()
                ?: throw IllegalArgumentException(
                    "No data source found for $entityType in system ${system.name}"
                )
        }

        return dataSourceFactoryRegistry.createDataSource(dataSourceConfig)
    }

    private fun validateAdHocRequest(request: AdHocReconciliationRequest) {
        val systemA = configLoader.getSourceSystem(request.sourceSystemA)
            ?: throw IllegalArgumentException("Source system A not found: ${request.sourceSystemA}")

        val systemB = configLoader.getSourceSystem(request.sourceSystemB)
            ?: throw IllegalArgumentException("Source system B not found: ${request.sourceSystemB}")

        val dsA = systemA.dataSources.find { it.name == request.dataSourceA }
            ?: throw IllegalArgumentException("DataSource A not found: ${request.dataSourceA}")

        if (!dsA.entityTypes.contains(request.entityType)) {
            throw IllegalArgumentException(
                "DataSource A '${request.dataSourceA}' doesn't support ${request.entityType}"
            )
        }

        val dsB = systemB.dataSources.find { it.name == request.dataSourceB }
            ?: throw IllegalArgumentException("DataSource B not found: ${request.dataSourceB}")

        if (!dsB.entityTypes.contains(request.entityType)) {
            throw IllegalArgumentException(
                "DataSource B '${request.dataSourceB}' doesn't support ${request.entityType}"
            )
        }
    }
}

