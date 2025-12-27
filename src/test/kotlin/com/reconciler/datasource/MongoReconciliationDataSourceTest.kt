package com.reconciler.datasource

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.bson.Document
import org.bson.conversions.Bson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class MongoReconciliationDataSourceTest {

    private lateinit var mongoClient: MongoClient
    private lateinit var database: MongoDatabase
    private lateinit var collection: MongoCollection<Document>
    private lateinit var dataSource: MongoReconciliationDataSource

    @BeforeEach
    fun setUp() {
        mongoClient = mock()
        database = mock()
        collection = mock()
        dataSource = MongoReconciliationDataSource(
            "mongo-ds",
            mongoClient,
            database,
            mapOf("QUOTE" to "quotes", "ORDER" to "orders")
        )
    }

    @Test
    fun `should get count successfully with map query`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = mapOf("tradeDate" to mapOf("\$eq" to "?tradeDate") as Any),
            parameters = null
        )

        whenever(database.getCollection("quotes")).thenReturn(collection)
        whenever(collection.countDocuments(any<Bson>())).thenReturn(1500L)

        val result = dataSource.getCount(EntityType.QUOTE, tradeDate, queryConfig)

        assertEquals(1500L, result)
        verify(collection).countDocuments(any<Bson>())
    }

    @Test
    fun `should get count successfully with string query`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = """{"tradeDate": {"${'$'}eq": "2024-01-15"}}""",
            parameters = null
        )

        whenever(database.getCollection("orders")).thenReturn(collection)
        whenever(collection.countDocuments(any<Bson>())).thenReturn(2000L)

        val result = dataSource.getCount(EntityType.ORDER, tradeDate, queryConfig)

        assertEquals(2000L, result)
    }

    @Test
    fun `should throw exception when collection not mapped`() {
        val dataSourceWithoutMapping = MongoReconciliationDataSource(
            "mongo-ds",
            mongoClient,
            database,
            emptyMap()
        )

        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = mapOf("tradeDate" to mapOf("\$eq" to "?tradeDate") as Any),
            parameters = null
        )

        assertThrows<IllegalArgumentException> {
            dataSourceWithoutMapping.getCount(EntityType.QUOTE, tradeDate, queryConfig)
        }
    }

    @Test
    fun `should throw exception when query is invalid type`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val queryConfig = QueryConfig(
            count = 12345, // Invalid type
            parameters = null
        )

        whenever(database.getCollection("quotes")).thenReturn(collection)

        assertThrows<IllegalArgumentException> {
            dataSource.getCount(EntityType.QUOTE, tradeDate, queryConfig)
        }
    }

    @Test
    fun `should return datasource name`() {
        assertEquals("mongo-ds", dataSource.getDataSourceName())
    }

    @Test
    fun `should return MONGODB type`() {
        assertEquals(DataSourceType.MONGODB, dataSource.getDataSourceType())
    }
}

