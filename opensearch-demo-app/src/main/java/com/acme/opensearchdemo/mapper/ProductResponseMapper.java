package com.acme.opensearchdemo.mapper;

import org.springframework.stereotype.Component;

import com.acme.common.mapper.AbstractOutboundMapperImpl;

import com.acme.opensearchdemo.dto.ProductResponse;
import com.acme.opensearchdemo.model.ProductDocument;

/**
 * Maps {@link ProductDocument} to {@link ProductResponse} for HTTP responses.
 * Response-only; request bodies still deserialize to {@code ProductDocument}
 * until a {@code ProductRequest} DTO exists.
 */
@Component
public class ProductResponseMapper extends AbstractOutboundMapperImpl<ProductDocument, ProductResponse> {

	@Override
	public ProductResponse toDto(ProductDocument entity) {
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
