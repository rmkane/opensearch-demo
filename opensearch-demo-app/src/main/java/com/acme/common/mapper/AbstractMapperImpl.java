package com.acme.common.mapper;

import org.springframework.lang.NonNull;

/**
 * Base class for bidirectional mappers. Subclasses must implement
 * {@link #toDto(Object)} and {@link #toEntity(Object)}; list helpers come from
 * {@link Mapper} defaults.
 *
 * @param <E>
 *            source / persistence type
 * @param <D>
 *            API DTO type
 * @see OutboundMapper
 * @see AbstractOutboundMapperImpl
 */
public abstract class AbstractMapperImpl<E, D> extends AbstractOutboundMapperImpl<E, D> implements Mapper<E, D> {

	@Override
	public abstract E toEntity(@NonNull D dto);
}
