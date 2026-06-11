package com.acme.opensearchdemo.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
public record ProductDocument(@Id String id,

		@Field(type = FieldType.Text) String name,

		@Field(type = FieldType.Keyword) String sku,

		@Field(type = FieldType.Double) BigDecimal price,

		@Field(type = FieldType.Date) Instant updatedOn) {
}
