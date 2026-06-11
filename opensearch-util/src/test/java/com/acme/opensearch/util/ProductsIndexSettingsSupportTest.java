package com.acme.opensearch.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductsIndexSettingsSupportTest {

	@Test
	void loadsSettingsFromClasspathJson() {
		ProductsIndexSettingsSupport.Values settings = ProductsIndexSettingsSupport.load();

		assertThat(settings.numberOfShards()).isEqualTo(1);
		assertThat(settings.numberOfReplicas()).isZero();
		assertThat(settings.refreshInterval()).isEqualTo("1s");
	}
}
