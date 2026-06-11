package com.acme.opensearchdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo app OpenSearch connection settings. Bound from {@code app.opensearch.*}
 * in {@code application.yml} (typically populated from {@code OPENSEARCH_*} env
 * vars via {@code local.env}). This is <em>not</em> Spring Boot's
 * {@code spring.opensearch.*} auto-configuration.
 */
@ConfigurationProperties(prefix = "app.opensearch")
public record OpenSearchProperties(String uri, String username, String password, boolean trustSelfSigned) {
}
