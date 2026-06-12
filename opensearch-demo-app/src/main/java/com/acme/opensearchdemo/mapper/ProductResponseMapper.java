package com.acme.opensearchdemo.mapper;

import org.springframework.stereotype.Component;

import com.acme.common.mapper.AbstractOutboundMapperImpl;

import com.acme.opensearch.model.Product;

import com.acme.opensearchdemo.dto.ProductResponse;

/**
 * Maps {@link Product} to {@link ProductResponse} for HTTP responses.
 * Response-only; request bodies still deserialize to {@code Product} until a
 * {@code ProductRequest} DTO exists.
 */
@Component
public class ProductResponseMapper extends AbstractOutboundMapperImpl<Product, ProductResponse> {

	@Override
	public ProductResponse toDto(Product entity) {
		/* spotless:off */
		return ProductResponse.builder()
				.id(entity.id())
				.name(entity.name())
				.sku(entity.sku())
				.price(entity.price())
				.updatedOn(entity.updatedOn())
				.build();
		/* spotless:on */
	}
}
