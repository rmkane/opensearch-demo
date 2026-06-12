package com.acme.opensearch.util.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo OpenSearch connection settings. Bound from {@code app.opensearch.*} in
 * the consuming application (typically populated from {@code OPENSEARCH_*} env
 * vars). This is <em>not</em> Spring Boot's {@code spring.opensearch.*}
 * auto-configuration.
 */
@ConfigurationProperties(prefix = "app.opensearch")
public record OpenSearchProperties(String uri, String username, String password, boolean trustSelfSigned) {
}
