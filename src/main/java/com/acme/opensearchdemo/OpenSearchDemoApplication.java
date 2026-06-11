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

@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class,
		ElasticsearchRestClientAutoConfiguration.class, OpenSearchRestHighLevelClientAutoConfiguration.class,
		OpenSearchClientAutoConfiguration.class, OpenSearchRestClientAutoConfiguration.class,
		ReactiveOpenSearchClientAutoConfiguration.class})
@EnableElasticsearchRepositories
public class OpenSearchDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenSearchDemoApplication.class, args);
	}
}
