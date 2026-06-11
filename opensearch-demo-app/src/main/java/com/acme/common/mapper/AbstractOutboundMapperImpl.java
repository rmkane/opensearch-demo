package com.acme.common.mapper;

/**
 * Base class for response-only mappers. Subclasses implement
 * {@link #toDto(Object)} only; list mapping is inherited from
 * {@link OutboundMapper#toDtoList(java.util.List)}.
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * &#64;Component
 * public class ProductResponseMapper extends AbstractOutboundMapperImpl<ProductDocument, ProductResponse> {
 *
 * 	&#64;Override
 * 	public ProductResponse toDto(ProductDocument entity) {
 * 		return ProductResponse.builder().id(entity.id())...build();
 * 	}
 * }
 * }
 * </pre>
 *
 * @param <E>
 *            source type
 * @param <D>
 *            API DTO type
 * @see Mapper
 * @see AbstractMapperImpl
 */
public abstract class AbstractOutboundMapperImpl<E, D> implements OutboundMapper<E, D> {
}
