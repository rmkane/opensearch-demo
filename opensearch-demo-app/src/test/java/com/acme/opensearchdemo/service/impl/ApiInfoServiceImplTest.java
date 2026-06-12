package com.acme.opensearchdemo.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.opensearchdemo.model.ApiInfo;

class ApiInfoServiceImplTest {

	@Test
	void fromServerPortBuildsLocalUrls() {
		ApiInfoServiceImpl service = new ApiInfoServiceImpl("opensearch-demo", "", "", "/swagger-ui.html",
				"/v3/api-docs", "/actuator");

		ApiInfo info = service.fromServerPort(8080);

		assertThat(info.application()).isEqualTo("opensearch-demo");
		assertThat(info.baseUrl()).isEqualTo("http://localhost:8080");
		assertThat(info.documentation().swaggerUi()).isEqualTo("http://localhost:8080/swagger-ui.html");
		assertThat(info.documentation().openApi()).isEqualTo("http://localhost:8080/v3/api-docs");
		assertThat(info.actuator().health()).isEqualTo("http://localhost:8080/actuator/health");
		assertThat(info.api().products()).isEqualTo("http://localhost:8080/api/products");
	}

	@Test
	void fromRequestPrefersConfiguredBaseUrl() {
		ApiInfoServiceImpl service = new ApiInfoServiceImpl("opensearch-demo", "https://demo.example.com/", "",
				"/swagger-ui.html", "/v3/api-docs", "/actuator");

		ApiInfo info = service.fromRequest("http://ignored:9999");

		assertThat(info.baseUrl()).isEqualTo("https://demo.example.com");
		assertThat(info.api().products()).isEqualTo("https://demo.example.com/api/products");
	}

	@Test
	void fromRequestUsesRequestBaseUrlWhenNotConfigured() {
		ApiInfoServiceImpl service = new ApiInfoServiceImpl("opensearch-demo", "", "", "/swagger-ui.html",
				"/v3/api-docs", "/actuator");

		ApiInfo info = service.fromRequest("http://localhost:8080/");

		assertThat(info.baseUrl()).isEqualTo("http://localhost:8080");
	}

	@Test
	void fromRequestFailsWhenBaseUrlCannotBeResolved() {
		ApiInfoServiceImpl service = new ApiInfoServiceImpl("opensearch-demo", "", "", "/swagger-ui.html",
				"/v3/api-docs", "/actuator");

		assertThatThrownBy(() -> service.fromRequest("")).isInstanceOf(IllegalStateException.class)
				.hasMessage("Unable to resolve API base URL");
	}
}
