# Reconciliation Service

A Kotlin Spring Boot application that reconciles quotes and orders from multiple data sources (Couchbase, MongoDB, and Oracle).

## Features

- **Multi-Datasource Support**: Reconciles data from Couchbase, MongoDB, and Oracle
- **Runtime Configuration**: All data sources and reconciliation rules configured via JSON
- **Ad-Hoc Reconciliation**: Create and execute reconciliation rules on-the-fly via REST API
- **Structured Logging**: JSON-formatted logs compatible with Kibana
- **REST API**: Complete REST API for managing and executing reconciliations

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/reconciler/
│   │   ├── config/          # Configuration loading and models
│   │   ├── factory/         # Data source factory implementations
│   │   ├── datasource/      # Data source implementations
│   │   ├── reconciliation/  # Core reconciliation logic
│   │   └── controller/       # REST API controllers
│   └── resources/
│       ├── application.properties
│       ├── reconciliation-config.json
│       └── logback-spring.xml
```

## Configuration

### 1. Data Source Configuration (`reconciliation-config.json`)

Define your source systems and their data sources:

```json
{
  "sourceSystems": [
    {
      "name": "system-a",
      "dataSources": [
        {
          "type": "COUCHBASE",
          "name": "couchbase-quotes",
          "connectionConfig": {
            "hosts": ["localhost:8091"],
            "bucket": "quotes-bucket",
            "username": "${couchbase.user}",
            "password": "${couchbase.password}"
          },
          "entityTypes": ["QUOTE"],
          "queries": {
            "QUOTE": {
              "count": "SELECT COUNT(*) as count FROM `quotes-bucket` WHERE tradeDate = $1"
            }
          }
        }
      ]
    }
  ]
}
```

### 2. Application Properties (`application.properties`)

Set connection credentials (use environment variables or override in `application-local.properties`):

```properties
couchbase.user=admin
couchbase.password=password
mongo.uri=mongodb://localhost:27017
oracle.url=jdbc:oracle:thin:@localhost:1521:XE
oracle.user=user
oracle.password=password
```

## Building and Running

### Build the project:
```bash
./gradlew build
```

### Run the application:
```bash
./gradlew bootRun
```

Or:
```bash
java -jar build/libs/reconciler-1.0.0.jar
```

The application will start on port 8080 by default.

## REST API Endpoints

### Get Available Reconciliation Rules
```http
GET /api/reconciliation/rules
```

### Get Source Systems
```http
GET /api/reconciliation/systems
```

### Get Available Reconciliations
```http
GET /api/reconciliation/available-reconciliations
```
Returns all possible reconciliation combinations between systems.

### Execute Predefined Rule
```http
POST /api/reconciliation/execute/{ruleName}
Content-Type: application/json

{
  "tradeDate": "2024-01-15"
}
```

### Execute Ad-Hoc Reconciliation
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

### Create Reconciliation Rule
```http
POST /api/reconciliation/rules
Content-Type: application/json

{
  "name": "quarterly-order-recon",
  "sourceSystemA": "system-a",
  "sourceSystemB": "system-b",
  "entityType": "ORDER",
  "tradeDateField": "tradeDate"
}
```

### Delete Reconciliation Rule
```http
DELETE /api/reconciliation/rules/{ruleName}
```

## Query Format

### Couchbase (N1QL)
```json
{
  "count": "SELECT COUNT(*) as count FROM `bucket-name` WHERE tradeDate = $1"
}
```

### MongoDB
```json
{
  "count": {
    "tradeDate": { "$eq": "?tradeDate" }
  }
}
```

### Oracle (SQL)
```json
{
  "count": "SELECT COUNT(*) as count FROM orders WHERE trade_date = :tradeDate"
}
```

## Logging

The application uses Logback with JSON encoding for Kibana integration. Logs are written to:
- Console (JSON format)
- File: `logs/reconciliation.log` (JSON format, rolling)

Reconciliation results are automatically logged in JSON format for easy parsing by Kibana.

## Example Usage

1. **Check available reconciliations:**
   ```bash
   curl http://localhost:8080/api/reconciliation/available-reconciliations
   ```

2. **Execute an ad-hoc reconciliation:**
   ```bash
   curl -X POST http://localhost:8080/api/reconciliation/execute-adhoc \
     -H "Content-Type: application/json" \
     -d '{
       "sourceSystemA": "system-a",
       "dataSourceA": "couchbase-quotes",
       "sourceSystemB": "system-b",
       "dataSourceB": "couchbase-quotes-b",
       "entityType": "QUOTE",
       "tradeDate": "2024-01-15",
       "persistRule": false
     }'
   ```

3. **Create and persist a rule:**
   ```bash
   curl -X POST http://localhost:8080/api/reconciliation/rules \
     -H "Content-Type: application/json" \
     -d '{
       "name": "daily-quote-recon",
       "sourceSystemA": "system-a",
       "sourceSystemB": "system-b",
       "entityType": "QUOTE",
       "tradeDateField": "tradeDate"
     }'
   ```

## Dependencies

- Spring Boot 3.2.0
- Kotlin 1.9.20
- Couchbase Java Client 3.4.9
- MongoDB Driver 4.11.1
- Oracle JDBC Driver 23.3.0
- HikariCP 5.1.0
- Logstash Logback Encoder 7.4

## Notes

- Data source connections are cached for performance
- Placeholders in `reconciliation-config.json` are resolved from system properties or environment variables
- Ad-hoc rules can be persisted for future use
- All reconciliation results are logged in JSON format for Kibana

