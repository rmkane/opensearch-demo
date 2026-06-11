package com.acme.opensearchdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OpenSearchPropertiesTest.Config.class)
@TestPropertySource(properties = {"app.opensearch.uri=https://search.example:443", "app.opensearch.username=admin",
		"app.opensearch.password=secret", "app.opensearch.trust-self-signed=false"})
class OpenSearchPropertiesTest {

	@Autowired
	private OpenSearchProperties properties;

	@Test
	void bindsAppOpenSearchProperties() {
		assertThat(properties.uri()).isEqualTo("https://search.example:443");
		assertThat(properties.username()).isEqualTo("admin");
		assertThat(properties.password()).isEqualTo("secret");
		assertThat(properties.trustSelfSigned()).isFalse();
	}

	@EnableConfigurationProperties(OpenSearchProperties.class)
	static class Config {
	}
}
