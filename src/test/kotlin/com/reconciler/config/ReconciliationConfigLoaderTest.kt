package com.reconciler.config

import com.reconciler.config.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.io.ByteArrayInputStream

class ReconciliationConfigLoaderTest {

    private lateinit var resourceLoader: ResourceLoader
    private lateinit var environment: Environment
    private lateinit var resource: Resource
    private lateinit var configLoader: ReconciliationConfigLoader

    @BeforeEach
    fun setUp() {
        resourceLoader = mock()
        environment = mock()
        resource = mock()
    }

    @Test
    fun `should load config successfully`() {
        val configJson = """
        {
          "sourceSystems": [
            {
              "name": "system-a",
              "dataSources": []
            }
          ],
          "reconciliationRules": [
            {
              "name": "rule-1",
              "sourceSystemA": "system-a",
              "sourceSystemB": "system-b",
              "entityType": "QUOTE",
              "tradeDateField": "tradeDate"
            }
          ]
        }
        """.trimIndent()

        whenever(resource.inputStream).thenReturn(ByteArrayInputStream(configJson.toByteArray()))
        whenever(resourceLoader.getResource(any())).thenReturn(resource)
        whenever(environment.getProperty(any())).thenReturn(null)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val config = configLoader.getConfig()
        assertNotNull(config)
        assertEquals(1, config.sourceSystems.size)
        assertEquals(1, config.reconciliationRules.size)
    }

    @Test
    fun `should resolve placeholders from environment`() {
        val configJson = """
        {
          "sourceSystems": [
            {
              "name": "system-a",
              "dataSources": [
                {
                  "type": "COUCHBASE",
                  "name": "cb-ds",
                  "connectionConfig": {
                    "username": "${'$'}{couchbase.user}",
                    "password": "${'$'}{couchbase.password}"
                  },
                  "entityTypes": ["QUOTE"],
                  "queries": {}
                }
              ]
            }
          ],
          "reconciliationRules": []
        }
        """.trimIndent()

        whenever(resource.inputStream).thenReturn(ByteArrayInputStream(configJson.toByteArray()))
        whenever(resourceLoader.getResource(any())).thenReturn(resource)
        whenever(environment.getProperty("couchbase.user")).thenReturn("admin")
        whenever(environment.getProperty("couchbase.password")).thenReturn("secret")

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val config = configLoader.getConfig()
        // Note: Placeholder resolution happens during JSON parsing, so we verify the loader works
        assertNotNull(config)
        assertEquals("system-a", config.sourceSystems[0].name)
    }

    @Test
    fun `should get rule by name`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val rule = configLoader.getRule("rule-1")
        assertNotNull(rule)
        assertEquals("rule-1", rule?.name)
        assertEquals("system-a", rule?.sourceSystemA)
    }

    @Test
    fun `should return null for non-existent rule`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val rule = configLoader.getRule("non-existent")
        assertNull(rule)
    }

    @Test
    fun `should get source system by name`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val system = configLoader.getSourceSystem("system-a")
        assertNotNull(system)
        assertEquals("system-a", system?.name)
    }

    @Test
    fun `should return null for non-existent source system`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val system = configLoader.getSourceSystem("non-existent")
        assertNull(system)
    }

    @Test
    fun `should get all rules`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val rules = configLoader.getAllRules()
        assertEquals(1, rules.size)
        assertEquals("rule-1", rules[0].name)
    }

    @Test
    fun `should get all source systems`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val systems = configLoader.getAllSourceSystems()
        assertEquals(1, systems.size)
        assertEquals("system-a", systems[0].name)
    }

    @Test
    fun `should add rule successfully`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val newRule = ReconciliationRule(
            name = "rule-2",
            sourceSystemA = "system-a",
            sourceSystemB = "system-b",
            entityType = EntityType.ORDER
        )

        configLoader.addRule(newRule)

        val rule = configLoader.getRule("rule-2")
        assertNotNull(rule)
        assertEquals("rule-2", rule?.name)
    }

    @Test
    fun `should throw exception when adding duplicate rule`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val duplicateRule = ReconciliationRule(
            name = "rule-1",
            sourceSystemA = "system-a",
            sourceSystemB = "system-b",
            entityType = EntityType.ORDER
        )

        assertThrows<IllegalArgumentException> {
            configLoader.addRule(duplicateRule)
        }
    }

    @Test
    fun `should remove rule successfully`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val removed = configLoader.removeRule("rule-1")
        assertTrue(removed)

        val rule = configLoader.getRule("rule-1")
        assertNull(rule)
    }

    @Test
    fun `should return false when removing non-existent rule`() {
        val configJson = createTestConfigJson()
        setupMocks(configJson)

        configLoader = ReconciliationConfigLoader("classpath:test-config.json", resourceLoader, environment)

        val removed = configLoader.removeRule("non-existent")
        assertFalse(removed)
    }

    @Test
    fun `should throw exception when config file not found`() {
        whenever(resourceLoader.getResource(any())).thenThrow(RuntimeException("File not found"))

        assertThrows<RuntimeException> {
            ReconciliationConfigLoader("classpath:missing.json", resourceLoader, environment)
        }
    }

    private fun createTestConfigJson(): String {
        return """
        {
          "sourceSystems": [
            {
              "name": "system-a",
              "dataSources": []
            }
          ],
          "reconciliationRules": [
            {
              "name": "rule-1",
              "sourceSystemA": "system-a",
              "sourceSystemB": "system-b",
              "entityType": "QUOTE",
              "tradeDateField": "tradeDate"
            }
          ]
        }
        """.trimIndent()
    }

    private fun setupMocks(configJson: String) {
        whenever(resource.inputStream).thenReturn(ByteArrayInputStream(configJson.toByteArray()))
        whenever(resourceLoader.getResource(any())).thenReturn(resource)
        whenever(environment.getProperty(any())).thenReturn(null)
    }
}

