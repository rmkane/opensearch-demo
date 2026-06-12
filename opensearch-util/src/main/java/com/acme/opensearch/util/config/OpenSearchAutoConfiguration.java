package com.acme.opensearch.util.config;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

import com.acme.opensearch.util.OpenSearchClientFactory;
import com.acme.opensearch.util.OpenSearchConnectionProperties;
import com.acme.opensearch.util.ProductIndexOperations;
import com.acme.opensearch.util.health.OpenSearchHealthIndicator;

/**
 * Registers a single {@link OpenSearchClient} and related beans from
 * {@code spring.opensearch.*}. Import via Spring Boot auto-configuration or
 * {@code @Import(OpenSearchAutoConfiguration.class)}.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public OpenSearchClient openSearchClient(OpenSearchProperties properties) {
		log.info("Using OpenSearch URI: {} (spring.opensearch.uris)", properties.primaryUri());
		OpenSearchConnectionProperties connection = new OpenSearchConnectionProperties(properties.primaryUri(),
				properties.username(), properties.password(), properties.trustSelfSigned());
		return OpenSearchClientFactory.create(connection);
	}

	@Bean
	@ConditionalOnMissingBean
	public ProductIndexOperations productIndexOperations(OpenSearchClient client) {
		return new ProductIndexOperations(client);
	}

	@Bean("opensearch")
	@ConditionalOnClass(HealthIndicator.class)
	@ConditionalOnMissingBean(name = "opensearch")
	public OpenSearchHealthIndicator openSearchHealthIndicator(OpenSearchClient client) {
		return new OpenSearchHealthIndicator(client);
	}
}
