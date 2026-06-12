<!-- omit in toc -->
# Troubleshooting

Notes from getting this demo running locally with Spring Boot 3.5.x, Actuator, and OpenSearch 3.x on host port 443.

<!-- omit in toc -->
## Contents

- [Version compatibility](#version-compatibility)
- [SDO 3.0 on Boot 3.5](#sdo-30-on-boot-35)
- [Duplicate `repositoryTagsProvider` bean](#duplicate-repositorytagsprovider-bean)
- [Connection to `localhost:9200`](#connection-to-localhost9200)
- [Local configuration (`local.env`)](#local-configuration-localenv)
- [OpenSearch admin password strength](#opensearch-admin-password-strength)
- [YAML passwords with `#`](#yaml-passwords-with-)
- [Actuator health](#actuator-health)
- [API compatibility (2.0.2 / 3.9.0)](#api-compatibility-202--390)
- [Strict mapping / `_class` on save](#strict-mapping--_class-on-save)
- [Strict mapping / missing `id` after `make down`](#strict-mapping--missing-id-after-make-down)
- [Quick run checklist](#quick-run-checklist)

## Version compatibility

| Component | Working version | Notes |
| --------- | --------------- | ----- |
| Spring Boot | 3.5.14 | Parent in `pom.xml` |
| Spring Data OpenSearch Starter | 2.0.2 | Matches Spring Boot 3.5.x |
| OpenSearch Java Client | 3.9.0 | Direct client API |
| OpenSearch Docker image | `opensearchproject/opensearch:3` | Currently 3.7.x |

## SDO 3.0 on Boot 3.5

Do not use `spring-data-opensearch` **3.0.x** with Spring Boot **3.5**. That line targets Boot 4.x and pulls in `spring-boot-data-*` 4.x modules, which clash with Boot 3.5 auto-configuration.

See [upgrade.md](upgrade.md) for the Boot 4 migration path.

## Duplicate `repositoryTagsProvider` bean

**Symptom**

```none
The bean 'repositoryTagsProvider' ... could not be registered.
A bean with that name has already been defined in
RepositoryMetricsAutoConfiguration ... and overriding is disabled.
```

**Cause**

`spring-data-opensearch` 3.0.5 + `spring-boot-starter-actuator` on Spring Boot 3.5.14 registers the same metrics beans twice (Boot 3.5 Actuator vs Boot 4.x data modules).

**Fix**

Pin `spring-data-opensearch.version` to `2.0.2` in `pom.xml`.

## Connection to `localhost:9200`

**Symptom**

```none
Connect to http://localhost:9200 failed: Connection refused
```

OpenSearch in this demo is only exposed on host port **443** (mapped to container 9200). Port 9200 is intentionally not published.

**Cause**

Several auto-configurations compete to create the Spring Data client:

1. Spring Boot’s `ElasticsearchDataAutoConfiguration` / `ElasticsearchRestClientAutoConfiguration`
2. OpenSearch’s `OpenSearchRestHighLevelClientAutoConfiguration` (REST high-level client → port 9200)
3. OpenSearch’s `OpenSearchClientAutoConfiguration` / `OpenSearchRestClientAutoConfiguration` (default REST transport)

These can register an `elasticsearchOperations` / `elasticsearchTemplate` bean before the custom Java client, ignoring `https://localhost:443`.

**Fix**

Exclude conflicting auto-config in `OpenSearchDemoApplication`:

- `ElasticsearchDataAutoConfiguration`
- `ElasticsearchRestClientAutoConfiguration`
- `OpenSearchRestHighLevelClientAutoConfiguration`
- `OpenSearchClientAutoConfiguration`
- `OpenSearchRestClientAutoConfiguration`
- `ReactiveOpenSearchClientAutoConfiguration`

Provide a single `OpenSearchClient` via `OpenSearchAutoConfiguration` in `opensearch-util` (reads `app.opensearch.*` from `OPENSEARCH_*` env vars). Spring Data’s `OpenSearchDataConfiguration.JavaClientConfiguration` then uses that bean.

## Local configuration (`local.env`)

Connection settings live in `local.env` at the repo root:

| Variable | Purpose |
| -------- | ------- |
| `OPENSEARCH_URI` | Cluster URL (`https://localhost:443`) |
| `OPENSEARCH_USERNAME` | Admin username |
| `OPENSEARCH_PASSWORD` | Admin password |
| `OPENSEARCH_TRUST_SELF_SIGNED` | Trust local self-signed TLS (`true` for Docker demo) |

Always `source local.env` before Compose, curl, or the app:

```bash
source local.env
```

`make up`, `make run`, and `make health` source `local.env` automatically.

`docker-compose.yml` uses `${OPENSEARCH_PASSWORD}` and `${OPENSEARCH_USERNAME}` with no defaults. Running `docker compose up` without sourcing `local.env` (or exporting those vars) passes empty values.

If a stale `OPENSEARCH_PASSWORD` is exported in your shell (e.g. an old `Admin123!`), Compose interpolation uses the shell value, not `local.env`. Either `source local.env` in the same shell or `unset OPENSEARCH_PASSWORD` first.

## OpenSearch admin password strength

**Symptom**

```none
Password Admin123! failed validation: "Weak password"
```

OpenSearch 3.7+ validates `OPENSEARCH_INITIAL_ADMIN_PASSWORD` with zxcvbn, not just character-class rules.

**Fix**

Use a stronger demo password in `local.env` (currently `Str0ng!Demo#9`). After changing the password, recycle the container and volume:

```bash
source local.env
docker compose down -v
docker compose up -d
```

`OPENSEARCH_INITIAL_ADMIN_PASSWORD` only applies on first cluster bootstrap; existing data volumes keep the old password.

## YAML passwords with `#`

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

## API compatibility (2.0.2 / 3.9.0)

When aligning code with the dependency set:

- Use `ElasticsearchRepository`, not `OpenSearchRepository` (removed in 2.0.2).
- Use `ElasticsearchOperations`, not `OpenSearchOperations` from the Spring Data Elasticsearch package.
- `numberOfShards` / `numberOfReplicas` take `Integer`, not `String`.
- `refreshInterval` expects `Time.of(t -> t.time("5s"))`, not a raw string.
- HttpClient 5 no longer has `setSSLContext` on `HttpAsyncClientBuilder`; configure TLS via `PoolingAsyncClientConnectionManager` + `TlsStrategy`.

## Strict mapping / `_class` on save

If `POST /api/products` returns 500 and the server log shows:

```text
strict_dynamic_mapping_exception ... dynamic introduction of [_class] within [_doc] is not allowed
```

The index was created with **strict** dynamic mapping (java-client path), but Spring Data tried to index a `_class` type-hint field that is not in the mapping.

**Fix:** on `Product`, use `@Document(..., writeTypeHint = WriteTypeHint.FALSE)` so saves only include mapped fields. Set `dynamic = Dynamic.STRICT` on `@Document` so Spring Data index creation matches the java-client path.

## Strict mapping / missing `id` after `make down`

If `POST /api/products` returns 500 after `make down` and `make up`, and the server log shows:

```text
strict_dynamic_mapping_exception ... dynamic introduction of [id] within [_doc] is not allowed
```

**Cause**

`make down` runs `docker compose down -v`, which deletes volumes and **all indices**. After `make up`, the `products` index is gone.

If the index is recreated via Spring Data (`make api-spring-index`, or app startup auto-create), the mapping includes only `@Field` properties (`name`, `sku`, `price`, `updatedOn`). Spring Data treats `@Id` as document metadata and does **not** add `id` to the mapping.

Saves still write `id` into `_source`. With `dynamic: strict` (from `settings.json`), OpenSearch rejects the unknown field.

The java-client path (`ProductIndexOperations`) explicitly maps `id` as `keyword`, so saves work when that index exists.

**Fix**

Recreate the index with the java-client mapping before saving products:

```bash
make api-recreate-java-index
```

Or call `PUT http://localhost:8080/api/products/index/java-client` while the app is running.

Integration tests call this endpoint in `shouldCreateIndex` before CRUD tests. After a fresh cluster, run the app first (`make run`), then integration tests.

**Verify mapping**

```bash
source local.env
curl -sk -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" \
  "$OPENSEARCH_URI/products" | jq '.products.mappings.properties | keys'
```

You should see `id` in the list. A Spring Data–only index omits it.

## Quick run checklist

```bash
source local.env
make up                        # wait for healthy container
make run                       # Spring Boot on :8080
make api-recreate-java-index   # products index with id mapping (required after make down -v)
make health                    # curl cluster health via :443
curl http://localhost:8080/actuator/health
```
