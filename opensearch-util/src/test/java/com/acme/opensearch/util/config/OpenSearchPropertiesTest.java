package com.acme.opensearch.util.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OpenSearchPropertiesTest.Config.class)
@TestPropertySource(properties = {
		/* spotless:off */
		"spring.opensearch.uris=https://search.example:443,https://search.example:444",
		"spring.opensearch.username=admin",
		"spring.opensearch.password=secret",
		"spring.opensearch.trust-self-signed=false"
		/* spotless:on */
})
class OpenSearchPropertiesTest {

	@Autowired
	private OpenSearchProperties properties;

	@Test
	void bindsSpringOpenSearchProperties() {
		assertThat(properties.uris()).containsExactly("https://search.example:443", "https://search.example:444");
		assertThat(properties.primaryUri()).isEqualTo("https://search.example:443");
		assertThat(properties.username()).isEqualTo("admin");
		assertThat(properties.password()).isEqualTo("secret");
		assertThat(properties.trustSelfSigned()).isFalse();
	}

	@EnableConfigurationProperties(OpenSearchProperties.class)
	static class Config {
	}
}
