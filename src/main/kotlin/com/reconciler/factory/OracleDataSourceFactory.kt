package com.reconciler.factory

import com.reconciler.config.models.DataSourceConfig
import com.reconciler.config.models.DataSourceType
import com.reconciler.datasource.OracleReconciliationDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class OracleDataSourceFactory : DataSourceFactory {
    override fun supports(type: DataSourceType): Boolean {
        return type == DataSourceType.ORACLE
    }

    override fun createDataSource(config: DataSourceConfig): OracleReconciliationDataSource {
        val connectionConfig = config.connectionConfig
        
        val jdbcUrl = connectionConfig["jdbcUrl"] as? String
            ?: throw IllegalArgumentException("Oracle JDBC URL not specified")
        
        val username = connectionConfig["username"] as? String
            ?: throw IllegalArgumentException("Oracle username not specified")
        
        val password = connectionConfig["password"] as? String
            ?: throw IllegalArgumentException("Oracle password not specified")

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "oracle.jdbc.OracleDriver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }

        val dataSource: DataSource = HikariDataSource(hikariConfig)

        return OracleReconciliationDataSource(
            dataSourceName = config.name,
            dataSource = dataSource
        )
    }
}

