# OpenSearch Demo — use `make` or `make help` for targets

APP_MODULE  := opensearch-demo-app
MVN         := mvn -pl $(APP_MODULE) -am
ARTIFACT    := $(shell mvn -pl $(APP_MODULE) help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
VERSION     := $(shell mvn -pl $(APP_MODULE) help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR         := $(APP_MODULE)/target/$(ARTIFACT)-$(VERSION).jar
MAIN_CLASS   := com.acme.opensearchdemo.OpenSearchDemoApplication
DEBUG_PORT   ?= 8787
JVM_DEBUG    := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$(DEBUG_PORT)
ENV          := set -a && . ./local.env && set +a &&
ID           ?= p-1

# JDK 24+: silence sun.misc.Unsafe warnings from Spotless on newer local JDKs
export MAVEN_OPTS ?= --sun-misc-unsafe-memory-access=allow

.PHONY: help develop verify build hooks \
        dev debug run jar prod \
        test compile format lint verify install \
        package build clean jar-path format-check \
        up down logs health \
        api-spring-index api-java-index api-recreate-java-index api-add-field api-refresh \
        api-save api-list api-get api-put api-patch api-delete api-purge

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
	mvn clean

## hooks: Install git pre-commit hook (Spotless + compile)
hooks:
	./scripts/install-git-hooks.sh

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
	mvn test

## compile: Compile main and test sources
compile:
	$(MVN) compile test-compile

## format: Apply code formatting (Spotless)
format:
	mvn spotless:apply -q

## lint: Check formatting and compile
lint: format-check compile

## format-check: Check formatting without applying changes
format-check:
	mvn spotless:check -q

## verify: Run tests and package
verify:
	mvn verify

# --- Build --------------------------------------------------------------------

##@ Build
## build: Build executable JAR (skip tests)
build: package

## package: Build executable JAR (skip tests)
package:
	$(MVN) package -DskipTests

## install: Install to local Maven repository
install:
	mvn install

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
	./scripts/curl/opensearch-health.sh

# --- API ----------------------------------------------------------------------

##@ API
## api-spring-index: Create index via Spring Data OpenSearch IndexOperations
api-spring-index:
	./scripts/curl/api-spring-index.sh

## api-java-index: Create index via direct opensearch-java client
api-java-index:
	./scripts/curl/api-java-index.sh

## api-recreate-java-index: Recreate index via direct opensearch-java client
api-recreate-java-index:
	./scripts/curl/api-recreate-java-index.sh

## api-add-field: Add description field to existing mapping using opensearch-java
api-add-field:
	./scripts/curl/api-add-field.sh

## api-refresh: Update refresh_interval to 5s using opensearch-java
api-refresh:
	./scripts/curl/api-refresh.sh

## api-save: Save a sample product document through Spring Data repository
api-save:
	./scripts/curl/api-save.sh

## api-list: List documents through Spring Data repository
api-list:
	./scripts/curl/api-list.sh

## api-get: Get product by ID (default: p-1; override with make api-get ID=p-2)
api-get:
	./scripts/curl/api-get.sh $(ID)

## api-put: Replace product by ID (default: p-1)
api-put:
	./scripts/curl/api-put.sh $(ID)

## api-patch: Partially update product by ID (default: p-1)
api-patch:
	./scripts/curl/api-patch.sh $(ID)

## api-delete: Delete product by ID (default: p-1; prints HTTP status)
api-delete:
	./scripts/curl/api-delete.sh $(ID)

## api-purge: Delete all product documents (index mapping is kept)
api-purge:
	./scripts/curl/api-purge.sh
