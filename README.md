# OpenSearch Spring Boot Demo

Multi-module Maven project:

| Module | Purpose |
| ------ | ------- |
| `opensearch-model` | Domain types and Spring Data entity (`Product`), repository (`ProductRepository`), index settings |
| `opensearch-util` | Java client factory, index operations, Spring auto-config (`OpenSearchAutoConfiguration`), actuator health |
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

Connection settings use the standard `spring.opensearch` property shape (from `OPENSEARCH_*` in `local.env`):

```yaml
spring:
  opensearch:
    uris: https://localhost:443
    username: admin
    password: "..."
```

Starter auto-config is excluded; `OpenSearchAutoConfiguration` in `opensearch-util` binds these properties. `trust-self-signed` is a demo-only extension for local Docker TLS.

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

## Create the index using Spring Data OpenSearch

```bash
make api-spring-index
```

or:

```bash
curl -X POST http://localhost:8080/api/products/index/spring-data
```

## Create/recreate the index using the direct Java client

```bash
make api-recreate-java-index
```

or:

```bash
curl -X PUT http://localhost:8080/api/products/index/java-client
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

For AWS OpenSearch, set the explicit port in `local.env` or `spring.opensearch.uris`:

```yaml
spring:
  opensearch:
    uris: https://your-domain.region.es.amazonaws.com:443
```

For production, do not use trust-all SSL. This sample trusts the local self-signed demo certificate only to simplify local development.
