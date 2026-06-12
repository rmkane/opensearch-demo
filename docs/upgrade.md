# Upgrade guide

This document describes how to move **opensearch-demo** from the current baseline to **Spring Boot 4** and **Spring Data OpenSearch 3.x**.

## Current baseline (this repo today)

| Component | Version |
| --------- | ------- |
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring Data OpenSearch Starter | 2.0.2 |
| OpenSearch Java Client | 3.9.0 |
| springdoc-openapi | 2.8.9 |
| OpenSearch Server (Docker) | 3.x |

## Target stack

| Component | Version | Notes |
| --------- | ------- | ----- |
| Java | 21+ | Required by both lines |
| Spring Boot | 4.0.x | Spring Framework 7.0.x |
| Spring Data OpenSearch Starter | 3.0.x | Spring Data release train **2025.1** |
| OpenSearch Java Client | 3.x | Align with SDO 3 release notes (e.g. 3.8+) |
| springdoc-openapi | 3.0.x | **Not** 2.x — 2.x is for Boot 3 |
| OpenSearch Server | 2.x / 3.x | Unchanged |

Official compatibility matrix: [spring-data-opensearch README](https://github.com/opensearch-project/spring-data-opensearch#compatibility-with-opensearch-and-spring).

## Important rule

**Do not** put `spring-data-opensearch` **3.0.x** on Spring Boot **3.5.x**.

The 3.0 starter depends on `spring-boot-data-*` / `spring-boot-elasticsearch` **4.x** modules. On Boot 3.5 you get classpath clashes, wrong auto-configuration, and startup failures. Stay on **2.0.x** until you upgrade Boot.

You **can** upgrade `opensearch-java` to 3.x **without** upgrading Spring Data OpenSearch — this project already does that on Boot 3.5.

## What changes vs what stays

| Area | Boot 3.5 → 4 | Action |
| ---- | ------------- | ------ |
| Parent `pom.xml` | Boot version, property versions | Update |
| `opensearch-model` | No Spring | Usually unchanged |
| `opensearch-util` | No Spring | Usually unchanged; bump `opensearch-java` if needed |
| `opensearch-demo-app` | Auto-config exclusions, actuator | Review and update |
| Index mapping / `settings.json` | OpenSearch API | Unchanged |
| DTO mappers (`docs/mapping.md`) | None | Unchanged |
| Integration tests | RestTemplate against running app | Unchanged |

## Step 1 — Bump parent and dependency versions

In the root `pom.xml`, update properties along these lines:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.x</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-data-opensearch.version>3.0.4</spring-data-opensearch.version>
    <opensearch-java.version>3.9.0</opensearch-java.version>  <!-- or version recommended by SDO 3.0.4 -->
    <springdoc.version>3.0.3</springdoc.version>
</properties>
```

Run a full compile after each bump:

```bash
mvn clean verify
```

## Step 2 — Auto-configuration exclusions (Boot 4)

Boot 4 moves some Elasticsearch integration classes into new packages. This project excludes competing Elasticsearch/OpenSearch auto-config so a single `OpenSearchClient` bean is created from `spring.opensearch.uris` (see `OpenSearchAutoConfiguration` in `opensearch-util`).

**Boot 3.5 (today)** — `OpenSearchDemoApplication.java`:

```java
@SpringBootApplication(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class,
    OpenSearchRestHighLevelClientAutoConfiguration.class,
    OpenSearchClientAutoConfiguration.class,
    OpenSearchRestClientAutoConfiguration.class,
    ReactiveOpenSearchClientAutoConfiguration.class
})
```

**Boot 4 + SDO 3.0** — use the classes documented for your exact Boot patch level. Names and packages changed; verify against compiler errors and [spring-data-opensearch configuration](https://docs.spring.io/spring-data/opensearch/reference/opensearch/configuration.html):

```java
@SpringBootApplication(exclude = {
    org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration.class,
    org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration.class,
    // Keep excluding OpenSearch starter clients that default to localhost:9200
    OpenSearchRestHighLevelClientAutoConfiguration.class,
    OpenSearchClientAutoConfiguration.class,
    OpenSearchRestClientAutoConfiguration.class,
    ReactiveOpenSearchClientAutoConfiguration.class
})
```

Equivalent `application.yml` (Boot 4):

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration
      - org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration
```

## Step 3 — Actuator health (Boot 4)

With `spring-boot-starter-actuator`, Boot 4 may register an **Elasticsearch** REST health indicator that probes `http://localhost:9200` even when you use OpenSearch on another host/port.

Symptom: actuator health shows Elasticsearch down / connection refused on 9200.

Fix: exclude Elasticsearch data and REST client auto-configuration (step 2). See [spring-data-opensearch#647](https://github.com/opensearch-project/spring-data-opensearch/issues/647).

This demo also provides `OpenSearchHealthIndicator` in `opensearch-util` for cluster health via the configured URI.

## Step 4 — Spring Data API notes (2.0.x → 3.0.x)

These conventions already apply on 2.0.2 and remain on 3.0.x:

- Repository interface: `ElasticsearchRepository<Document, Id>` (not `OpenSearchRepository`).
- Template API: `ElasticsearchOperations` (from `org.springframework.data.elasticsearch.core`).
- `@EnableElasticsearchRepositories` on the application class.

After upgrade, re-check:

- `IndexOperations` / `createWithMapping()` for index creation.
- `Product` annotations: `@Document`, `@Setting`, `writeTypeHint = WriteTypeHint.FALSE`, `dynamic = Dynamic.STRICT` (see `docs/troubleshooting.md`).

## Step 5 — springdoc (2.x → 3.x)

| Boot line | springdoc artifact line |
| --------- | ------------------------ |
| 3.x | `springdoc-openapi-starter-webmvc-ui` **2.8.x** |
| 4.x | `springdoc-openapi-starter-webmvc-ui` **3.0.x** |

Swagger UI and OpenAPI JSON paths in this project (`/swagger-ui.html`, `/v3/api-docs`) should stay the same; confirm after upgrade.

## Step 6 — Verify locally

```bash
source local.env
make up
make health                    # OpenSearch cluster via :443
mvn clean verify               # unit tests (integration tests excluded by default)
make run                       # app on :8080
curl http://localhost:8080/actuator/health
make api-spring-index          # or api-recreate-java-index
make api-save
make api-list
make api-get ID=p-1
```

Run integration tests (OpenSearch + app must be running):

```bash
mvn -pl opensearch-demo-app test -Pintegration

# one class
mvn -pl opensearch-demo-app test -Pintegration \
  -Dtest=ProductSearchControllerIntegrationTest
```

Or `make test-integration` from the repo root (requires `make dev` in another terminal).

## Step 7 — Optional: patch-level updates without Boot 4

If you are **not** upgrading to Boot 4 yet, you can still safely:

- Bump **Spring Boot 3.5.x** patch (e.g. 3.5.14 → latest 3.5.x).
- Bump **spring-data-opensearch 2.0.x** patch (stay on 2.0 line).
- Bump **opensearch-java** 3.x (client only).
- Bump **springdoc 2.8.x** (stay on 2.x line).

```bash
mvn versions:display-property-updates   # inspect available bumps
mvn clean verify
```

## Rollback

Work on a branch. If Boot 4 migration blocks you:

1. Revert `pom.xml` parent and property versions to Boot 3.5 + SDO 2.0.x.
2. Restore Boot 3 auto-config exclusion imports in `OpenSearchDemoApplication`.
3. `mvn clean verify` and `make run`.

## References

- [Spring Data OpenSearch — compatibility](https://github.com/opensearch-project/spring-data-opensearch#compatibility-with-opensearch-and-spring)
- [Spring Data OpenSearch — configuration](https://docs.spring.io/spring-data/opensearch/reference/opensearch/configuration.html)
- [springdoc-openapi releases](https://github.com/springdoc/springdoc-openapi/releases)
- Local issues: [docs/troubleshooting.md](troubleshooting.md)
- DTO mapping (unchanged by Boot upgrade): [docs/mapping.md](mapping.md)
