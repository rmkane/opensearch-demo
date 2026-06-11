package com.acme.opensearchdemo.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

import com.acme.opensearch.model.Product;
import com.acme.opensearch.model.ProductsIndex;
import com.acme.opensearch.model.ProductsIndexSettings;

/**
 * Spring Data OpenSearch mapping for {@link Product}.
 * <p>
 * Annotated documents stay in the demo (or a future spring-data module); the
 * plain {@link Product} record lives in {@code opensearch-model} so it can be
 * shared across client implementations.
 */
@Document(indexName = ProductsIndex.INDEX_NAME, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.STRICT)
@Setting(settingPath = ProductsIndexSettings.SETTINGS_PATH)
public record ProductDocument(@Id String id,

		@Field(type = FieldType.Text) String name,

		@Field(type = FieldType.Keyword) String sku,

		@Field(type = FieldType.Double) BigDecimal price,

		@Field(type = FieldType.Date) Instant updatedOn) {
}
