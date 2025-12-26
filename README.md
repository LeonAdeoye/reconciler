# Reconciliation Service

A Kotlin Spring Boot application designed for **End of Day (EOD) reconciliations** between different data sources across multiple systems. This service enables you to compare record counts (quotes, orders, etc.) from different systems at the end of a trading day to ensure data consistency and identify discrepancies.

## Purpose

The Reconciliation Service is used to perform EOD reconciliations between different datasources in different systems. For example, you may want to:

- Get a count of orders at the end of the day on a particular trade date from System A (e.g., stored in Couchbase)
- Compare the same count of orders from System B (e.g., stored in MongoDB or Oracle)
- Verify that both systems have the same number of records for a given trade date

Since the two systems use different datasources, the queries required to extract the counts may be different. This application handles that by allowing you to configure system-specific queries while comparing the same logical data.

### Key Use Cases

- **Order Reconciliation**: Compare order counts between trading systems at EOD
- **Quote Reconciliation**: Verify quote counts match across different quote systems
- **Cross-System Validation**: Ensure data consistency between primary and backup systems
- **Ad-Hoc Reconciliation**: Perform one-off reconciliations between any two configured systems

## Features

- **Multi-Datasource Support**: Reconciles data from Couchbase, MongoDB, and Oracle
- **Runtime Configuration**: All data sources and reconciliation rules configured via JSON (no code changes needed)
- **Flexible Query Configuration**: Each datasource can have its own query format (N1QL, MongoDB queries, SQL)
- **Ad-Hoc Reconciliation**: Create and execute reconciliation rules on-the-fly via REST API
- **Structured Logging**: JSON-formatted logs compatible with Kibana for monitoring and alerting
- **REST API**: Complete REST API for managing and executing reconciliations

## Class Structure

The application follows a layered architecture with clear separation of concerns:

### Configuration Layer (`com.reconciler.config`)
- **`ReconciliationConfigLoader`**: Loads and parses the `reconciliation-config.json` file, resolves placeholders, and provides runtime access to configuration
- **`QueryProvider`**: Retrieves query configurations for specific datasources and entity types
- **Models** (`config.models`): Data classes representing configuration structure:
  - `ReconciliationConfig`: Root configuration container
  - `SourceSystemConfig`: Represents a system with its datasources
  - `DataSourceConfig`: Configuration for a single datasource (connection info, queries)
  - `ReconciliationRule`: Defines what to reconcile (which systems, entity type, date field)

### Factory Layer (`com.reconciler.factory`)
- **`DataSourceFactory`**: Interface for creating datasource connections
- **`DataSourceFactoryRegistry`**: Manages all factory implementations and creates datasources on demand
- **Factory Implementations**:
  - `CouchbaseDataSourceFactory`: Creates Couchbase cluster and bucket connections
  - `MongoDataSourceFactory`: Creates MongoDB client and database connections
  - `OracleDataSourceFactory`: Creates Oracle JDBC datasource using HikariCP

### Data Source Layer (`com.reconciler.datasource`)
- **`ReconciliationDataSource`**: Interface for executing count queries
- **Implementations**:
  - `CouchbaseReconciliationDataSource`: Executes N1QL queries against Couchbase
  - `MongoReconciliationDataSource`: Executes MongoDB queries and counts documents
  - `OracleReconciliationDataSource`: Executes SQL queries against Oracle using JdbcTemplate

### Reconciliation Layer (`com.reconciler.reconciliation`)
- **`ReconciliationService`**: Core service that orchestrates reconciliation execution
  - Executes predefined rules or ad-hoc reconciliations
  - Retrieves datasources, executes queries, compares counts
- **`ReconciliationExecutor`**: Executes multiple reconciliation rules in batch
- **`ReconciliationLogger`**: Logs reconciliation results in JSON format for Kibana
- **`ReconciliationResult`**: Data class containing reconciliation results (counts, match status, difference)

### Controller Layer (`com.reconciler.controller`)
- **`ReconciliationController`**: REST API endpoints for:
  - Listing available rules and systems
  - Executing reconciliations
  - Creating/deleting rules
  - Discovering available reconciliation combinations

### Data Flow

1. **Configuration Loading**: `ReconciliationConfigLoader` loads JSON config at startup
2. **Rule Execution**: When a reconciliation is triggered:
   - `ReconciliationService` retrieves the rule configuration
   - Gets datasource configurations for both systems
   - `DataSourceFactoryRegistry` creates datasource instances (cached)
   - `QueryProvider` retrieves the appropriate query for each datasource
   - Each datasource executes its query and returns a count
   - Counts are compared and a result is generated
   - Result is logged via `ReconciliationLogger`

## Building the Application

### Prerequisites

- Java 21 (configured via Gradle toolchain)
- Gradle 7.6+ (or use Gradle Wrapper)

### Build Commands

**Build the project:**
```bash
./gradlew build
```

**Run the application:**
```bash
./gradlew bootRun
```

**Build a JAR file:**
```bash
./gradlew bootJar
```

**Run the JAR:**
```bash
java -jar build/libs/reconciler-1.0.0.jar
```

The application will start on port 8080 by default (configurable in `application.properties`).

### Build Configuration

The project uses:
- **Java 21** (configured via Gradle toolchain)
- **Kotlin 1.9.24**
- **Spring Boot 3.2.5**
- **Gradle Kotlin DSL** (`build.gradle.kts`)

The build automatically downloads Java 21 if not found (via Foojay toolchain resolver plugin).

## Reconciliation Configuration JSON

The `reconciliation-config.json` file is the central configuration for all reconciliation rules and datasources. It defines:

### Structure

```json
{
  "sourceSystems": [
    {
      "name": "system-a",
      "dataSources": [
        {
          "type": "COUCHBASE|MONGODB|ORACLE",
          "name": "unique-datasource-name",
          "connectionConfig": { /* connection details */ },
          "entityTypes": ["QUOTE", "ORDER"],
          "queries": {
            "QUOTE": { /* query config */ },
            "ORDER": { /* query config */ }
          }
        }
      ]
    }
  ],
  "reconciliationRules": [
    {
      "name": "rule-name",
      "sourceSystemA": "system-a",
      "sourceSystemB": "system-b",
      "entityType": "QUOTE|ORDER",
      "tradeDateField": "tradeDate"
    }
  ]
}
```

### Source Systems

Each `sourceSystem` represents a logical system (e.g., "trading-system-a", "backup-system-b") and contains:

- **`name`**: Unique identifier for the system
- **`dataSources`**: Array of datasources belonging to this system

### Data Sources

Each `dataSource` configuration includes:

- **`type`**: One of `COUCHBASE`, `MONGODB`, or `ORACLE`
- **`name`**: Unique name for this datasource (used in ad-hoc reconciliations)
- **`connectionConfig`**: Connection parameters (varies by type):
  - **Couchbase**: `hosts`, `bucket`, `username`, `password`
  - **MongoDB**: `uri`, `database`, `collection` (or `{entityType}_collection`)
  - **Oracle**: `jdbcUrl`, `username`, `password`
- **`entityTypes`**: Array of entity types this datasource supports (`QUOTE`, `ORDER`)
- **`queries`**: Map of entity type to query configuration

### Query Configuration

Queries are defined per entity type and datasource. Each query configuration includes:

- **`count`**: The query to execute (format depends on datasource type)
- **`parameters`**: Optional parameter definitions

#### Couchbase (N1QL)
```json
{
  "QUOTE": {
    "count": "SELECT COUNT(*) as count FROM `quotes-bucket` WHERE tradeDate = $1",
    "parameters": {
      "tradeDate": "DATE"
    }
  }
}
```

#### MongoDB
```json
{
  "ORDER": {
    "count": {
      "tradeDate": { "$eq": "?tradeDate" }
    },
    "parameters": {
      "tradeDate": "DATE"
    }
  }
}
```

#### Oracle (SQL)
```json
{
  "ORDER": {
    "count": "SELECT COUNT(*) as count FROM orders WHERE trade_date = :tradeDate",
    "parameters": {
      "tradeDate": "DATE"
    }
  }
}
```

### Reconciliation Rules

Rules define what to reconcile:

- **`name`**: Unique rule identifier
- **`sourceSystemA`** / **`sourceSystemB`**: Systems to compare
- **`entityType`**: Type of entity to reconcile (`QUOTE` or `ORDER`)
- **`tradeDateField`**: Field name used for date filtering (default: "tradeDate")

### Placeholder Resolution

Connection configs can use placeholders that are resolved from:
1. Spring `application.properties`
2. Environment variables
3. System properties

Example:
```json
{
  "username": "${couchbase.user}",
  "password": "${couchbase.password}"
}
```

Resolved from `application.properties`:
```properties
couchbase.user=admin
couchbase.password=secret123
```

## HTTP API Requests

The application provides a REST API for managing and executing reconciliations. All endpoints are under `/api/reconciliation`.

### Get Available Reconciliation Rules

Retrieve all configured reconciliation rules.

**Request:**
```http
GET /api/reconciliation/rules
```

**Response:**
```json
[
  {
    "name": "quote-reconciliation",
    "sourceSystemA": "system-a",
    "sourceSystemB": "system-b",
    "entityType": "QUOTE",
    "tradeDateField": "tradeDate"
  }
]
```

### Get Source Systems

Retrieve all configured source systems with their datasources.

**Request:**
```http
GET /api/reconciliation/systems
```

**Response:**
```json
{
  "system-a": {
    "name": "system-a",
    "dataSources": [
      {
        "name": "couchbase-quotes",
        "type": "COUCHBASE",
        "entityTypes": ["QUOTE"]
      }
    ]
  }
}
```

### Get Available Reconciliations

Discover all possible reconciliation combinations between systems.

**Request:**
```http
GET /api/reconciliation/available-reconciliations
```

**Response:**
```json
[
  {
    "sourceSystemA": "system-a",
    "sourceSystemB": "system-b",
    "availableEntityTypes": ["QUOTE", "ORDER"],
    "dataSourcePairs": {
      "QUOTE": [
        {
          "dataSourceA": "couchbase-quotes",
          "dataSourceB": "couchbase-quotes-b"
        }
      ],
      "ORDER": [
        {
          "dataSourceA": "mongo-orders",
          "dataSourceB": "oracle-orders"
        }
      ]
    }
  }
]
```

### Execute Predefined Rule

Execute a reconciliation using a predefined rule.

**Request:**
```http
POST /api/reconciliation/execute/{ruleName}
Content-Type: application/json

{
  "tradeDate": "2024-01-15"
}
```

**Response:**
```json
{
  "result": {
    "ruleName": "quote-reconciliation",
    "sourceSystemA": "system-a",
    "sourceSystemB": "system-b",
    "entityType": "QUOTE",
    "tradeDate": "2024-01-15",
    "countA": 1500,
    "countB": 1500,
    "match": true,
    "difference": 0,
    "timestamp": "2024-01-15T23:59:59Z",
    "dataSourceA": "couchbase-quotes",
    "dataSourceB": "couchbase-quotes-b"
  },
  "ruleCreated": false,
  "ruleName": "quote-reconciliation"
}
```

### Execute Ad-Hoc Reconciliation

Create and execute a reconciliation on-the-fly without a predefined rule.

**Request:**
```http
POST /api/reconciliation/execute-adhoc
Content-Type: application/json

{
  "ruleName": "my-custom-recon",
  "sourceSystemA": "system-a",
  "dataSourceA": "couchbase-quotes",
  "sourceSystemB": "system-b",
  "dataSourceB": "mongo-quotes",
  "entityType": "QUOTE",
  "tradeDate": "2024-01-15",
  "tradeDateField": "tradeDate",
  "persistRule": true
}
```

**Request Fields:**
- `ruleName`: Optional name for the rule (auto-generated if empty)
- `sourceSystemA` / `sourceSystemB`: System names from config
- `dataSourceA` / `dataSourceB`: Specific datasource names to use
- `entityType`: `QUOTE` or `ORDER`
- `tradeDate`: Date to reconcile (ISO format: `YYYY-MM-DD`)
- `tradeDateField`: Field name for date filtering (default: "tradeDate")
- `persistRule`: If `true`, saves the rule for future use

**Response:** Same as Execute Predefined Rule

### Create Reconciliation Rule

Create a new reconciliation rule (without executing it).

**Request:**
```http
POST /api/reconciliation/rules
Content-Type: application/json

{
  "name": "daily-order-recon",
  "sourceSystemA": "system-a",
  "sourceSystemB": "system-b",
  "entityType": "ORDER",
  "tradeDateField": "tradeDate"
}
```

**Response:** `201 Created` with the created rule

### Delete Reconciliation Rule

Remove a reconciliation rule.

**Request:**
```http
DELETE /api/reconciliation/rules/{ruleName}
```

**Response:** `200 OK` if deleted, `404 Not Found` if rule doesn't exist

## Example Usage

### 1. Check Available Reconciliations

```bash
curl http://localhost:8080/api/reconciliation/available-reconciliations
```

### 2. Execute EOD Order Reconciliation

```bash
curl -X POST http://localhost:8080/api/reconciliation/execute-adhoc \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystemA": "system-a",
    "dataSourceA": "mongo-orders",
    "sourceSystemB": "system-b",
    "dataSourceB": "oracle-orders",
    "entityType": "ORDER",
    "tradeDate": "2024-01-15",
    "persistRule": false
  }'
```

### 3. Create and Execute a Daily Rule

```bash
# Create the rule
curl -X POST http://localhost:8080/api/reconciliation/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "daily-quote-recon",
    "sourceSystemA": "system-a",
    "sourceSystemB": "system-b",
    "entityType": "QUOTE",
    "tradeDateField": "tradeDate"
  }'

# Execute it
curl -X POST http://localhost:8080/api/reconciliation/execute/daily-quote-recon \
  -H "Content-Type: application/json" \
  -d '{
    "tradeDate": "2024-01-15"
  }'
```

## Logging

The application uses Logback with JSON encoding for Kibana integration:

- **Console**: Human-readable format for development
- **File**: JSON format in `logs/reconciliation.log` (rolling, max 100MB per file, 30 day retention)
- **Reconciliation Results**: Automatically logged in JSON format with all reconciliation details

Log entries include:
- Event type (`reconciliation_result`)
- Rule name, systems, entity type
- Trade date, counts from both systems
- Match status, difference
- Timestamp, datasource names

## Dependencies

- Spring Boot 3.2.5
- Kotlin 1.9.24
- Couchbase Java Client 3.4.9
- MongoDB Driver 4.11.1
- Oracle JDBC Driver 23.3.0
- HikariCP 5.1.0
- Logstash Logback Encoder 7.4

## Notes

- Data source connections are cached for performance (created once per datasource name)
- Placeholders in `reconciliation-config.json` are resolved from Spring properties or environment variables
- Ad-hoc rules can be persisted for future use via the `persistRule` flag
- All reconciliation results are logged in JSON format for Kibana monitoring and alerting
- The application excludes Spring Boot's default DataSource auto-configuration to avoid conflicts with dynamic datasources
