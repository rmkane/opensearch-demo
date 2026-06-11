package com.acme.opensearch.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain product type shared across OpenSearch client implementations (no
 * Spring annotations).
 */
public record Product(String id, String name, String sku, BigDecimal price, Instant updatedOn) {
}
