package com.reconciler.factory


import com.couchbase.client.java.Bucket
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.ClusterOptions
import com.couchbase.client.java.env.ClusterEnvironment
import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.datasource.CouchbaseReconciliationDataSource
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CouchbaseDataSourceFactory : DataSourceFactory {
    override fun supports(type: DataSourceType): Boolean {
        return type == DataSourceType.COUCHBASE
    }

    override fun createDataSource(config: DataSourceConfig): CouchbaseReconciliationDataSource {
        val connectionConfig = config.connectionConfig
        
        val hosts = (connectionConfig["hosts"] as? List<*>)?.map { it.toString() }
            ?: throw IllegalArgumentException("Couchbase hosts not specified")
        
        val bucketName = connectionConfig["bucket"] as? String
            ?: throw IllegalArgumentException("Couchbase bucket not specified")
        
        val username = connectionConfig["username"] as? String
            ?: throw IllegalArgumentException("Couchbase username not specified")
        
        val password = connectionConfig["password"] as? String
            ?: throw IllegalArgumentException("Couchbase password not specified")

        val environment = ClusterEnvironment.builder()
            .timeoutConfig { builder ->
                builder.kvTimeout(Duration.ofSeconds(10))
                builder.queryTimeout(Duration.ofSeconds(30))
            }
            .build()

        val connectionString = hosts.joinToString(",")
        val cluster = Cluster.connect(
            connectionString,
            ClusterOptions.clusterOptions(username, password)
                .environment(environment)
        )

        val bucket: Bucket = cluster.bucket(bucketName)
        bucket.waitUntilReady(Duration.ofSeconds(10))

        return CouchbaseReconciliationDataSource(
            dataSourceName = config.name,
            cluster = cluster,
            bucket = bucket
        )
    }
}

