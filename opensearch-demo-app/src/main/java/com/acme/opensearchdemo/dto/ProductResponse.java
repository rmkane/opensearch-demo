package com.acme.opensearchdemo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

/**
 * API representation of a product returned by {@code /api/products} endpoints.
 * Mapped from {@link com.acme.opensearchdemo.model.ProductDocument} via
 * {@link com.acme.opensearchdemo.mapper.ProductResponseMapper}.
 */
@Data
@Builder
public class ProductResponse {
	private String id;
	private String name;
	private String sku;
	private BigDecimal price;
	private Instant updatedOn;
}
