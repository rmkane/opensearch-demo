package com.acme.common.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.opensearch.core.common.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Bidirectional mapper between an internal type {@code E} and an API DTO
 * {@code D}. Extend {@link AbstractMapperImpl} when you need both
 * {@link #toDto(Object)} and {@link #toEntity(Object)} (typical for request
 * DTOs that round-trip to entities).
 * <p>
 * For response-only mapping, prefer {@link OutboundMapper} and
 * {@link AbstractOutboundMapperImpl} — no unused {@code toEntity} stub
 * required.
 * <p>
 * This is a lightweight, hand-written alternative to MapStruct for this demo:
 * explicit field mapping in Java, no annotation processor, easy to debug. When
 * mappers grow or you need compile-time validation, consider migrating
 * individual mappers to MapStruct while keeping the same interface shape.
 *
 * @param <E>
 *            source / persistence type
 * @param <D>
 *            API DTO type
 * @see OutboundMapper
 * @see AbstractMapperImpl
 */
public interface Mapper<E, D> extends OutboundMapper<E, D> {

	E toEntity(@NonNull D dto);

	/**
	 * Maps a list of DTOs, skipping null elements. Returns an empty list when
	 * {@code dtos} is null or empty.
	 */
	default List<E> toEntityList(@Nullable List<D> dtos) {
		if (CollectionUtils.isEmpty(dtos)) {
			return Collections.emptyList();
		}
		/* spotless:off */
		return dtos.stream()
				.filter(Objects::nonNull)
				.map(this::toEntity)
				.toList();
		/* spotless:on */
	}
}
