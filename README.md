# OpenSearch Spring Boot Demo

Multi-module Maven project:

| Module | Purpose |
| ------ | ------- |
| `opensearch-model` | Shared domain types (`Product`, `ProductsIndex`, `ApiInfo`) |
| `opensearch-util` | Framework-free OpenSearch helpers (`OpenSearchClientFactory`, `ProductIndexOperations`) |
| `opensearch-demo-app` | Spring Boot demo app and HTTP API |

DTO mapping conventions: [docs/mapping.md](docs/mapping.md)

Upgrade path (Boot 3.5 → Boot 4 / SDO 3.0): [docs/upgrade.md](docs/upgrade.md)

Build and run from the repo root:

```bash
source local.env
make up
make run          # mvn -pl opensearch-demo-app -am spring-boot:run
make verify       # all modules
```

This is a minimal Spring Boot 3.5.x / Java 21 project that demonstrates:

- `spring-data-opensearch-starter`
- direct `opensearch-java` client usage
- Spring Data repository usage
- creating/recreating an index
- adding a mapping field
- updating index settings
- Docker Compose mapping local host port `443` to OpenSearch container port `9200`

This demo intentionally disables Spring Boot Elasticsearch/OpenSearch auto-configuration and provides one explicit `OpenSearchClient` bean. This avoids default fallback behavior to `localhost:9200` and allows testing AWS-style HTTPS endpoints on port `443`. See [docs/troubleshooting.md](docs/troubleshooting.md) for the exclusion list and rationale.

Connection settings are bound via `@ConfigurationProperties` under `app.opensearch` (not `spring.opensearch.*`):

```yaml
app:
  opensearch:
    uri: ${OPENSEARCH_URI}          # https://localhost:443
    username: ${OPENSEARCH_USERNAME}
    password: "${OPENSEARCH_PASSWORD}"
    trust-self-signed: ${OPENSEARCH_TRUST_SELF_SIGNED:true}
```

On startup the app logs the resolved URI, e.g. `Using OpenSearch URI: https://localhost:443`.

The Docker Compose file exposes only:

```yaml
ports:
  - "443:9200"
```

That makes it easier to verify whether the app really connects through HTTPS port 443 instead of falling back to 9200.

Connection settings live in `local.env`. Source it before running Compose, curl, or the app:

```bash
source local.env
```

## Versions

- Java: 21
- Spring Boot: 3.5.5
- Spring Data OpenSearch Starter: 2.0.2
- OpenSearch Java Client: 3.9.0
- OpenSearch Docker image: `opensearchproject/opensearch:3`

## Start OpenSearch

```bash
source local.env
make up
make health
```

Equivalent health check:

```bash
source local.env
curl -k -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" "$OPENSEARCH_URI/_cluster/health?pretty"
```

## Run the app

```bash
source local.env
make run
```

## Create/recreate the index (recommended — java-client)

Use the java-client path for the canonical mapping (includes the `id` field required by strict saves):

```bash
make api-recreate-java-index
```

or:

```bash
curl -X PUT http://localhost:8080/api/products/index/java-client
```

## Create the index using Spring Data OpenSearch (comparison only)

Spring Data index creation omits `id` from the mapping; use this endpoint to compare approaches, not for CRUD against a strict index. See [docs/troubleshooting.md](docs/troubleshooting.md#strict-mapping--missing-id-after-make-down).

```bash
make api-spring-index
```

or:

```bash
curl -X POST http://localhost:8080/api/products/index/spring-data
```

## Add a new mapping field using the direct Java client

```bash
make api-add-field
```

This adds a `description` field to the existing index mapping.

Important: OpenSearch allows adding fields to an existing mapping, but it does not allow changing the type of an existing field in-place. For that, create a new index and reindex.

## Update an index setting using the direct Java client

```bash
make api-refresh
```

This updates `index.refresh_interval` to `5s`.

## Save a document using the Spring Data repository

```bash
make api-save
make api-list
```

## Manual sample document

```bash
curl -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"id":"p-2","name":"Notebook","sku":"NOTE-002","price":8.50}'
```

## Port test

With Compose running, this should work:

```bash
source local.env
curl -k -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" "$OPENSEARCH_URI"
```

This should fail, because host port 9200 is intentionally not mapped:

```bash
source local.env
curl -k -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" "https://localhost:9200"
```

If your application tries `https://localhost:9200`, you have reproduced the unwanted default-port behavior.

## Notes

For AWS OpenSearch, set the explicit port in `local.env` or `app.opensearch.uri`:

```yaml
app:
  opensearch:
    uri: https://your-domain.region.es.amazonaws.com:443
```

For production, do not use trust-all SSL. This sample trusts the local self-signed demo certificate only to simplify local development.
