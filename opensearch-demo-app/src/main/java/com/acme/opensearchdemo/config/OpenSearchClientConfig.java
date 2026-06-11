package com.acme.opensearchdemo.config;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import com.acme.opensearch.util.OpenSearchClientFactory;
import com.acme.opensearch.util.OpenSearchConnectionProperties;
import com.acme.opensearch.util.ProductIndexOperations;

/**
 * Spring wiring for the shared {@link OpenSearchClient}.
 * <p>
 * Reads {@link OpenSearchProperties} ({@code app.opensearch.*}), not Boot's
 * {@code spring.opensearch.*} auto-config. The client factory lives in
 * {@code opensearch-util}; this class only exposes beans to the demo app.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchClientConfig {

	@Bean
	public OpenSearchClient openSearchClient(OpenSearchProperties properties) {
		log.info("Using OpenSearch URI: {}", properties.uri());
		OpenSearchConnectionProperties connection = new OpenSearchConnectionProperties(properties.uri(),
				properties.username(), properties.password(), properties.trustSelfSigned());
		return OpenSearchClientFactory.create(connection);
	}

	@Bean
	public ProductIndexOperations productIndexOperations(OpenSearchClient client) {
		return new ProductIndexOperations(client);
	}
}
