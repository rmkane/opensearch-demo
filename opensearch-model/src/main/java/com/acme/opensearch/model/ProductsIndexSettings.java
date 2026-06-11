package com.acme.opensearch.model;

import lombok.experimental.UtilityClass;

/**
 * Classpath location of shared index settings for
 * {@link ProductsIndex#INDEX_NAME}.
 */
@UtilityClass
public final class ProductsIndexSettings {

	public static final String SETTINGS_PATH = "/opensearch/products/settings.json";
}
