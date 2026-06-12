package com.acme.opensearch.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Product domain type and Spring Data OpenSearch entity for the products index.
 */
@Document(indexName = Product.INDEX_NAME, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.STRICT)
@Setting(settingPath = ProductsIndexSettings.SETTINGS_PATH)
public record Product(
		/* spotless:off */
	@Id String id,
	@Field(type = FieldType.Text) String name,
	@Field(type = FieldType.Keyword) String sku,
	@Field(type = FieldType.Double) BigDecimal price,
	@Field(type = FieldType.Date) Instant updatedOn
) {
	/* spotless:on */

	public static final String INDEX_NAME = "products";
}
