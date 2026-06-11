# Troubleshooting

Notes from getting this demo running locally with Spring Boot 3.5.x, Actuator, and OpenSearch 3.x on host port 443.

## Table of Contents

- [Troubleshooting](#troubleshooting)
  - [Table of Contents](#table-of-contents)
  - [Version compatibility](#version-compatibility)
    - [Do not use `spring-data-opensearch` 3.0.x with Spring Boot 3.5. That line targets Spring Boot 4.x and pulls in `spring-boot-data-*` 4.x modules, which clash with Boot 3.5 auto-configuration.](#do-not-use-spring-data-opensearch-30x-with-spring-boot-35-that-line-targets-spring-boot-4x-and-pulls-in-spring-boot-data--4x-modules-which-clash-with-boot-35-auto-configuration)
  - [Startup failure: duplicate `repositoryTagsProvider` bean](#startup-failure-duplicate-repositorytagsprovider-bean)
    - [Symptom](#symptom)
    - [Cause](#cause)
    - [Fix](#fix)
  - [Startup failure: connection to `http://localhost:9200`](#startup-failure-connection-to-httplocalhost9200)
    - [Symptom](#symptom-1)
    - [Cause](#cause-1)
    - [Fix](#fix-1)
  - [Local configuration: `local.env`](#local-configuration-localenv)
    - [Always source before Compose, curl, or the app:](#always-source-before-compose-curl-or-the-app)
  - [OpenSearch admin password strength](#opensearch-admin-password-strength)
    - [Symptom](#symptom-2)
    - [Fix](#fix-2)
  - [YAML: quote passwords containing `#`](#yaml-quote-passwords-containing-)
  - [Actuator health](#actuator-health)
  - [API compatibility notes (2.0.2 / 3.9.0)](#api-compatibility-notes-202--390)
  - [Quick run checklist](#quick-run-checklist)

## Version compatibility

| Component | Working version | Notes |
| --------- | --------------- | ----- |
| Spring Boot | 3.5.14 | Parent in `pom.xml` |
| Spring Data OpenSearch Starter | ### 2.0.2 | Matches Spring Boot 3.5.x |
| OpenSearch Java Client | 3.9.0 | Direct client API |
| OpenSearch Docker image | `opensearchproject/opensearch:3` | Currently 3.7.x |

### Do not use `spring-data-opensearch` 3.0.x with Spring Boot 3.5. That line targets Spring Boot 4.x and pulls in `spring-boot-data-*` 4.x modules, which clash with Boot 3.5 auto-configuration.

## Startup failure: duplicate `repositoryTagsProvider` bean

### Symptom

```none
The bean 'repositoryTagsProvider' ... could not be registered.
A bean with that name has already been defined in
RepositoryMetricsAutoConfiguration ... and overriding is disabled.
```

### Cause

`spring-data-opensearch` 3.0.5 + `spring-boot-starter-actuator` on Spring Boot 3.5.14 registers the same metrics beans twice (Boot 3.5 Actuator vs Boot 4.x data modules).

### Fix

Pin `spring-data-opensearch.version` to `2.0.2` in `pom.xml`.

## Startup failure: connection to `http://localhost:9200`

### Symptom

```none
Connect to http://localhost:9200 failed: Connection refused
```

OpenSearch in this demo is only exposed on ### host port 443 (mapped to container 9200). Port 9200 is intentionally not published.

### Cause

Several auto-configurations compete to create the Spring Data client:

1. Spring Boot’s `ElasticsearchDataAutoConfiguration` / `ElasticsearchRestClientAutoConfiguration`
2. OpenSearch’s `OpenSearchRestHighLevelClientAutoConfiguration` (REST high-level client → port 9200)
3. OpenSearch’s `OpenSearchClientAutoConfiguration` / `OpenSearchRestClientAutoConfiguration` (default REST transport)

These can register an `elasticsearchOperations` / `elasticsearchTemplate` bean ### before the custom Java client, ignoring `https://localhost:443`.

### Fix

Exclude conflicting auto-config in `OpenSearchDemoApplication`:

- `ElasticsearchDataAutoConfiguration`
- `ElasticsearchRestClientAutoConfiguration`
- `OpenSearchRestHighLevelClientAutoConfiguration`
- `OpenSearchClientAutoConfiguration`
- `OpenSearchRestClientAutoConfiguration`
- `ReactiveOpenSearchClientAutoConfiguration`

Provide a single `OpenSearchClient` in `OpenSearchClientConfig` that reads `OPENSEARCH_*` from the environment. Spring Data’s `OpenSearchDataConfiguration.JavaClientConfiguration` then uses that bean.

## Local configuration: `local.env`

Connection settings live in `local.env` at the repo root:

| Variable | Purpose |
| -------- | ------- |
| `OPENSEARCH_URI` | Cluster URL (`https://localhost:443`) |
| `OPENSEARCH_USERNAME` | Admin username |
| `OPENSEARCH_PASSWORD` | Admin password |
| `OPENSEARCH_TRUST_SELF_SIGNED` | Trust local self-signed TLS (`true` for Docker demo) |

### Always source before Compose, curl, or the app:

```bash
source local.env
```

`make up`, `make run`, and `make health` source `local.env` automatically.

`docker-compose.yml` uses `${OPENSEARCH_PASSWORD}` and `${OPENSEARCH_USERNAME}` with ### no defaults. Running `docker compose up` without sourcing `local.env` (or exporting those vars) passes empty values.

If a stale `OPENSEARCH_PASSWORD` is exported in your shell (e.g. an old `Admin123!`), Compose interpolation uses the shell value, not `local.env`. Either `source local.env` in the same shell or `unset OPENSEARCH_PASSWORD` first.

## OpenSearch admin password strength

### Symptom

```none
Password Admin123! failed validation: "Weak password"
```

OpenSearch 3.7+ validates `OPENSEARCH_INITIAL_ADMIN_PASSWORD` with zxcvbn, not just character-class rules.

### Fix

Use a stronger demo password in `local.env` (currently `Str0ng!Demo#9`). After changing the password, recycle the container and volume:

```bash
source local.env
docker compose down -v
docker compose up -d
```

`OPENSEARCH_INITIAL_ADMIN_PASSWORD` only applies on first cluster bootstrap; existing data volumes keep the old password.

## YAML: quote passwords containing `#`

In `application.yml`, unquoted `#` starts a YAML comment:

```yaml
# Broken — everything after # is ignored
password: ${OPENSEARCH_PASSWORD:Str0ng!Demo#9}

# Correct
password: "${OPENSEARCH_PASSWORD:Str0ng!Demo#9}"
```

## Actuator health

Actuator is enabled via `spring-boot-starter-actuator`. Health is exposed at:

```none
http://localhost:8080/actuator/health
```

Configured in `application.yml` with `show-details: always` for local debugging.

## API compatibility notes (2.0.2 / 3.9.0)

When aligning code with the dependency set:

- Use `ElasticsearchRepository`, not `OpenSearchRepository` (removed in 2.0.2).
- Use `ElasticsearchOperations`, not `OpenSearchOperations` from the Spring Data Elasticsearch package.
- `numberOfShards` / `numberOfReplicas` take `Integer`, not `String`.
- `refreshInterval` expects `Time.of(t -> t.time("5s"))`, not a raw string.
- HttpClient 5 no longer has `setSSLContext` on `HttpAsyncClientBuilder`; configure TLS via `PoolingAsyncClientConnectionManager` + `TlsStrategy`.

## Quick run checklist

```bash
source local.env
make up          # wait for healthy container
make run         # Spring Boot on :8080
make health      # curl cluster health via :443
curl http://localhost:8080/actuator/health
```
