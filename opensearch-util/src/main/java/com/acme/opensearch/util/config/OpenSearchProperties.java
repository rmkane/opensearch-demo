package com.acme.opensearch.util.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * OpenSearch connection settings using the same property names as
 * {@code org.opensearch.spring.boot.autoconfigure.OpenSearchProperties}
 * ({@code spring.opensearch.*}). Auto-config from the starter is excluded in
 * this demo; {@link OpenSearchAutoConfiguration} reads these properties
 * instead.
 * <p>
 * {@code trust-self-signed} is a demo-only extension for local Docker TLS.
 */
@ConfigurationProperties(prefix = "spring.opensearch")
public record OpenSearchProperties(List<String> uris, String username, String password, boolean trustSelfSigned) {

	public String primaryUri() {
		Assert.notEmpty(uris, "spring.opensearch.uris must not be empty");
		return uris.getFirst();
	}
}
