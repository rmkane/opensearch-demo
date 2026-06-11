package com.acme.opensearch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenSearchClientFactoryTest {

	@Test
	void rejectsUnsupportedUriScheme() {
		OpenSearchConnectionProperties properties = new OpenSearchConnectionProperties("ftp://opensearch.local",
				"admin", "secret", false);

		assertThatThrownBy(() -> OpenSearchClientFactory.create(properties))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported OpenSearch URI scheme");
	}
}
