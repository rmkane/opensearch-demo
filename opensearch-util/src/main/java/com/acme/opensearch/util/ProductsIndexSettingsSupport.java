package com.acme.opensearch.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.acme.opensearch.model.ProductsIndex;
import com.acme.opensearch.model.ProductsIndexSettings;

/**
 * Loads {@link ProductsIndexSettings#SETTINGS_PATH} for the opensearch-java
 * client. Spring Data reads the same file via {@code @Setting} on
 * {@code ProductDocument}.
 */
public final class ProductsIndexSettingsSupport {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ProductsIndexSettingsSupport() {
	}

	public record Values(int numberOfShards, int numberOfReplicas, String refreshInterval) {
	}

	public static Values load() {
		try (InputStream in = ProductsIndex.class.getResourceAsStream(ProductsIndexSettings.SETTINGS_PATH)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + ProductsIndexSettings.SETTINGS_PATH);
			}
			JsonNode index = MAPPER.readTree(in).get("index");
			return new Values(index.get("number_of_shards").asInt(), index.get("number_of_replicas").asInt(),
					index.get("refresh_interval").asText());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load " + ProductsIndexSettings.SETTINGS_PATH, e);
		}
	}
}
