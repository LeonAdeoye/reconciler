package com.reconciler.datasource

import com.couchbase.client.java.Bucket
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.query.QueryResult
import com.reconciler.config.models.DataSourceType
import com.reconciler.config.models.EntityType
import com.reconciler.config.models.QueryConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class CouchbaseReconciliationDataSourceTest {

    private lateinit var cluster: Cluster
    private lateinit var bucket: Bucket
    private lateinit var queryResult: QueryResult
    private lateinit var dataSource: CouchbaseReconciliationDataSource

    @BeforeEach
    fun setUp() {
        cluster = mock()
        bucket = mock()
        queryResult = mock()
        dataSource = CouchbaseReconciliationDataSource("cb-ds", cluster, bucket)
    }

    @Test
    fun `should get count successfully`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val parameters = mapOf("tradeDate" to tradeDate.toString())
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) as count FROM `bucket` WHERE tradeDate = $tradeDate",
            parameters = mapOf("tradeDate" to "DATE")
        )

        val mockRow = mock<com.couchbase.client.java.json.JsonObject>()
        whenever(mockRow.get("count")).thenReturn(1500L)
        whenever(queryResult.rowsAsObject()).thenReturn(listOf(mockRow))
        whenever(cluster.query(any(), any())).thenReturn(queryResult)

        val result = dataSource.getCount(EntityType.QUOTE, parameters, queryConfig)

        assertEquals(1500L, result)
        verify(cluster).query(any(), any())
    }

    @Test
    fun `should return zero when no rows returned`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val parameters = mapOf("tradeDate" to tradeDate.toString())
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) as count FROM `bucket` WHERE tradeDate = $tradeDate",
            parameters = mapOf("tradeDate" to "DATE")
        )

        whenever(queryResult.rowsAsObject()).thenReturn(emptyList())
        whenever(cluster.query(any(), any())).thenReturn(queryResult)

        val result = dataSource.getCount(EntityType.QUOTE, parameters, queryConfig)

        assertEquals(0L, result)
    }

    @Test
    fun `should handle string count value`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val parameters = mapOf("tradeDate" to tradeDate.toString())
        val queryConfig = QueryConfig(
            count = "SELECT COUNT(*) as count FROM `bucket` WHERE tradeDate = $tradeDate",
            parameters = mapOf("tradeDate" to "DATE")
        )

        val mockRow = mock<com.couchbase.client.java.json.JsonObject>()
        whenever(mockRow.get("count")).thenReturn("2000")
        whenever(queryResult.rowsAsObject()).thenReturn(listOf(mockRow))
        whenever(cluster.query(any(), any())).thenReturn(queryResult)

        val result = dataSource.getCount(EntityType.QUOTE, parameters, queryConfig)

        assertEquals(2000L, result)
    }

    @Test
    fun `should throw exception when query is not a string`() {
        val tradeDate = java.time.LocalDate.of(2024, 1, 15)
        val parameters = mapOf("tradeDate" to tradeDate.toString())
        val queryConfig = QueryConfig(
            count = mapOf("field" to "value"), // Not a string
            parameters = mapOf("tradeDate" to "DATE")
        )

        assertThrows<IllegalArgumentException> {
            dataSource.getCount(EntityType.QUOTE, parameters, queryConfig)
        }
    }

    @Test
    fun `should return datasource name`() {
        assertEquals("cb-ds", dataSource.getDataSourceName())
    }

    @Test
    fun `should return COUCHBASE type`() {
        assertEquals(DataSourceType.COUCHBASE, dataSource.getDataSourceType())
    }
}

