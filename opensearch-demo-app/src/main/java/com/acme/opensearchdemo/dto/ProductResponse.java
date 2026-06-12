package com.acme.opensearchdemo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of a product returned by {@code /api/products} endpoints.
 * Mapped from {@link com.acme.opensearch.model.Product} via
 * {@link com.acme.opensearchdemo.mapper.ProductResponseMapper}.
 */
@Data
@Builder
@Schema(description = "Product returned by /api/products endpoints")
public class ProductResponse {

	@Schema(description = "Unique product identifier", example = "p-1")
	private String id;

	@Schema(description = "Display name", example = "Coffee Mug")
	private String name;

	@Schema(description = "Stock-keeping unit", example = "MUG-001")
	private String sku;

	@Schema(description = "Unit price", example = "12.99")
	private BigDecimal price;

	@Schema(description = "Last update timestamp (ISO-8601); set by the server on save", example = "2026-06-10T12:00:00Z")
	private Instant updatedOn;
}
