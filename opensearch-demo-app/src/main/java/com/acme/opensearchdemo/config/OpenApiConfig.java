package com.acme.opensearchdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		/* spotless:off */
		return new OpenAPI()
				.info(new Info()
				.title("OpenSearch Demo API")
				.description("Spring Boot demo for spring-data-opensearch and opensearch-java")
				.version("v1"));
		/* spotless:on */
	}
}
