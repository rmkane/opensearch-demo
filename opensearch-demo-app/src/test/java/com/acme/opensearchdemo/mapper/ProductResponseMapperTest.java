package com.acme.opensearchdemo.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.opensearchdemo.model.ProductDocument;

class ProductResponseMapperTest {

	private final ProductResponseMapper mapper = new ProductResponseMapper();

	@Test
	void mapsAllFields() {
		Instant updatedOn = Instant.parse("2026-06-10T12:00:00Z");
		ProductDocument document = new ProductDocument("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"),
				updatedOn);

		var response = mapper.toDto(document);

		assertThat(response.getId()).isEqualTo("p-1");
		assertThat(response.getName()).isEqualTo("Coffee Mug");
		assertThat(response.getSku()).isEqualTo("MUG-001");
		assertThat(response.getPrice()).isEqualByComparingTo("12.99");
		assertThat(response.getUpdatedOn()).isEqualTo(updatedOn);
	}

	@Test
	void toDtoListReturnsEmptyForNull() {
		assertThat(mapper.toDtoList(null)).isEmpty();
	}

	@Test
	void toDtoListSkipsNullElements() {
		ProductDocument document = new ProductDocument("p-1", "Coffee Mug", "MUG-001", BigDecimal.ONE, null);

		assertThat(mapper.toDtoList(java.util.Arrays.asList(document, null))).hasSize(1);
	}
}
