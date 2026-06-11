package com.acme.opensearchdemo;

import org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.ReactiveOpenSearchClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Demo application entry point.
 * <p>
 * Several Boot and OpenSearch auto-configurations compete to create a client
 * and default to {@code http://localhost:9200}. Excluding them lets
 * {@code OpenSearchClientConfig} supply a single {@code OpenSearchClient} from
 * {@code OPENSEARCH_URI} (typically {@code https://localhost:443}). Spring
 * Data's {@code OpenSearchDataConfiguration.JavaClientConfiguration} then
 * reuses that bean. See {@code docs/troubleshooting.md} for details.
 */
@SpringBootApplication(exclude = {
		/* spotless:off */
	// Boot Elasticsearch data + REST clients (would ignore OPENSEARCH_URI / port 443)
	ElasticsearchDataAutoConfiguration.class,
	ElasticsearchRestClientAutoConfiguration.class,
	// OpenSearch starter clients (HLRC, default REST transport, reactive)
	OpenSearchRestHighLevelClientAutoConfiguration.class,
	OpenSearchClientAutoConfiguration.class,
	OpenSearchRestClientAutoConfiguration.class,
	ReactiveOpenSearchClientAutoConfiguration.class
	/* spotless:on */
})
@EnableElasticsearchRepositories
public class OpenSearchDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenSearchDemoApplication.class, args);
	}
}
