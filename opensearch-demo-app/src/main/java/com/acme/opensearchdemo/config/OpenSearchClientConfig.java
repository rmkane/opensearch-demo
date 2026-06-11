package com.acme.opensearchdemo.config;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.acme.opensearch.util.OpenSearchClientFactory;
import com.acme.opensearch.util.OpenSearchConnectionProperties;
import com.acme.opensearch.util.ProductIndexOperations;

/**
 * Spring wiring for the shared {@link OpenSearchClient}.
 * <p>
 * Reads {@code OPENSEARCH_URI}, {@code OPENSEARCH_USERNAME},
 * {@code OPENSEARCH_PASSWORD}, and {@code OPENSEARCH_TRUST_SELF_SIGNED} from
 * the environment (see {@code local.env}). The client factory lives in
 * {@code opensearch-util}; this class only exposes beans to the demo app.
 */
@Configuration
public class OpenSearchClientConfig {

	@Bean
	public OpenSearchClient openSearchClient(
			/* spotless:off */
			@Value("${OPENSEARCH_URI}") String uri,
			@Value("${OPENSEARCH_USERNAME}") String username,
			@Value("${OPENSEARCH_PASSWORD}") String password,
			@Value("${OPENSEARCH_TRUST_SELF_SIGNED:true}") boolean trustSelfSigned
			/* spotless:on */
	) {
		OpenSearchConnectionProperties properties = new OpenSearchConnectionProperties(uri, username, password,
				trustSelfSigned);
		return OpenSearchClientFactory.create(properties);
	}

	@Bean
	public ProductIndexOperations productIndexOperations(OpenSearchClient client) {
		return new ProductIndexOperations(client);
	}
}
