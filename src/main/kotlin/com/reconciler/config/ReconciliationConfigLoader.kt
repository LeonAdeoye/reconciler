package com.reconciler.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.reconciler.config.models.ReconciliationConfig
import com.reconciler.config.models.ReconciliationRule
import com.reconciler.config.models.SourceSystemConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

@Service
class ReconciliationConfigLoader(
    @Value("\${reconciliation.config.path:classpath:reconciliation-config.json}")
    private val configPath: String,
    private val resourceLoader: ResourceLoader,
    private val environment: Environment
) {
    private lateinit var config: ReconciliationConfig
    private val ruleMap = ConcurrentHashMap<String, ReconciliationRule>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    init {
        config = loadConfig()
        config.reconciliationRules.forEach { rule ->
            ruleMap[rule.name] = rule
        }
    }

    fun getConfig(): ReconciliationConfig = config

    fun getRule(ruleName: String): ReconciliationRule? {
        return ruleMap[ruleName]
    }

    fun getSourceSystem(name: String): SourceSystemConfig? {
        return config.sourceSystems.find { it.name == name }
    }

    fun getAllRules(): List<ReconciliationRule> {
        return ruleMap.values.toList()
    }

    fun getAllSourceSystems(): List<SourceSystemConfig> {
        return config.sourceSystems
    }

    fun addRule(rule: ReconciliationRule) {
        if (ruleMap.containsKey(rule.name)) {
            throw IllegalArgumentException("Rule with name '${rule.name}' already exists")
        }
        ruleMap[rule.name] = rule
    }

    fun removeRule(ruleName: String): Boolean {
        return ruleMap.remove(ruleName) != null
    }

    private fun loadConfig(): ReconciliationConfig {
        return try {
            val resource = resourceLoader.getResource(configPath)
            val inputStream: InputStream = resource.inputStream
            val configJson = inputStream.bufferedReader().use { it.readText() }
            
            // Resolve placeholders using Spring Environment
            val resolvedJson = resolvePlaceholders(configJson)
            
            objectMapper.readValue<ReconciliationConfig>(resolvedJson)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load reconciliation config from $configPath", e)
        }
    }

    private fun resolvePlaceholders(json: String): String {
        // Resolve placeholders using Spring Environment (supports properties and environment variables)
        var resolved = json
        val pattern = Regex("\\$\\{([^}]+)}")
        
        pattern.findAll(json).forEach { match ->
            val key = match.groupValues[1]
            val value = environment.getProperty(key) ?: System.getenv(key) ?: ""
            resolved = resolved.replace(match.value, value)
        }
        
        return resolved
    }
}

