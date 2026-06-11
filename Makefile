# OpenSearch Demo — use `make` or `make help` for targets

MVN          := mvn
ARTIFACT     := $(shell $(MVN) help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
VERSION      := $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR          := target/$(ARTIFACT)-$(VERSION).jar
MAIN_CLASS   := com.acme.opensearchdemo.OpenSearchDemoApplication
DEBUG_PORT   ?= 8787
JVM_DEBUG    := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$(DEBUG_PORT)
ENV          := set -a && . ./local.env && set +a &&

# JDK 24+: silence sun.misc.Unsafe warnings from Spotless on newer local JDKs
export MAVEN_OPTS ?= --sun-misc-unsafe-memory-access=allow

.PHONY: help develop verify build \
        dev debug run jar prod \
        test compile format lint verify install \
        package build clean jar-path format-check \
        up down logs health \
        spring-index java-index recreate-java-index add-field refresh save list

.DEFAULT_GOAL := help

# --- Workspace ----------------------------------------------------------------

##@ Workspace
## help: List targets
help:
	@printf '%s\n' 'OpenSearch Demo'
	@awk '/^##@ / { printf "\n%s\n", substr($$0, 5); next } \
	     /^## [a-z]/ { line=$$0; sub(/^## /,"",line); split(line,a,": "); \
	       printf "  %-22s %s\n", a[1], a[2] }' $(MAKEFILE_LIST)
	@printf '\nExamples:\n  make up dev   # OpenSearch + Spring Boot\n  make jar        # packaged JAR\n  make verify     # tests + package\n'

## jar-path: Print path to the packaged JAR
jar-path:
	@echo $(JAR)

## clean: Remove build output
clean:
	$(MVN) clean

# --- Develop ------------------------------------------------------------------

##@ Develop
## dev: Run via Spring Boot (sources local.env); compile first so IDE-stale classes are not used
dev: compile
	$(ENV) $(MVN) spring-boot:run

## debug: Run with remote debugging on port $(DEBUG_PORT) (sources local.env)
debug: compile
	$(ENV) $(MVN) spring-boot:run \
		-Dspring-boot.run.jvmArguments="$(JVM_DEBUG)"

## run: Alias for dev
run: dev

## jar: Build and run the fat JAR (sources local.env)
jar: package
	$(ENV) java -jar $(JAR)

## prod: Alias for jar
prod: jar

# --- Verify -------------------------------------------------------------------

##@ Verify
## test: Run unit tests
test:
	$(MVN) test

## compile: Compile main and test sources
compile:
	$(MVN) compile test-compile

## format: Apply code formatting (Spotless)
format:
	$(MVN) spotless:apply -q

## lint: Check formatting and compile
lint: format-check compile

## format-check: Check formatting without applying changes
format-check:
	$(MVN) spotless:check -q

## verify: Run tests and package
verify:
	$(MVN) verify

# --- Build --------------------------------------------------------------------

##@ Build
## build: Build executable JAR (skip tests)
build: package

## package: Build executable JAR (skip tests)
package:
	$(MVN) package -DskipTests

## install: Install to local Maven repository
install:
	$(MVN) install

# --- OpenSearch ---------------------------------------------------------------

##@ OpenSearch
## up: Start local OpenSearch on https://localhost:443 (sources local.env)
up:
	$(ENV) docker compose up -d

## down: Stop local OpenSearch
down:
	docker compose down -v

## logs: Tail OpenSearch logs
logs:
	docker compose logs -f opensearch

## health: Check OpenSearch through host port 443 (sources local.env)
health:
	$(ENV) curl -k -u "$$OPENSEARCH_USERNAME:$$OPENSEARCH_PASSWORD" "$$OPENSEARCH_URI/_cluster/health?pretty"

# --- API ----------------------------------------------------------------------

##@ API
## spring-index: Create index via Spring Data OpenSearch IndexOperations
spring-index:
	curl -s -X POST http://localhost:8080/api/products/index/spring-data | jq .

## java-index: Create index via direct opensearch-java client
java-index:
	curl -s -X POST http://localhost:8080/api/products/index/java-client | jq .

## recreate-java-index: Recreate index via direct opensearch-java client
recreate-java-index:
	curl -s -X PUT http://localhost:8080/api/products/index/java-client | jq .

## add-field: Add description field to existing mapping using opensearch-java
add-field:
	curl -s -X PUT http://localhost:8080/api/products/index/java-client/mapping/description | jq .

## refresh: Update refresh_interval to 5s using opensearch-java
refresh:
	curl -s -X PUT http://localhost:8080/api/products/index/java-client/settings/refresh-interval/5s | jq .

## save: Save a sample product document through Spring Data repository
save:
	curl -s -X POST http://localhost:8080/api/products \
		-H 'Content-Type: application/json' \
		-d '{"id":"p-1","name":"Coffee Mug","sku":"MUG-001","price":12.99}' | jq .

## list: List documents through Spring Data repository
list:
	curl -s http://localhost:8080/api/products | jq .
