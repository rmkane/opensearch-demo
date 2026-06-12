package com.acme.common.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Maps an internal type {@code E} (persistence entity or domain object) to an
 * API DTO {@code D}. This is the usual choice for controller response mapping
 * when you do not need to convert request DTOs back into entities.
 * <p>
 * Compare with {@link Mapper} for bidirectional mapping (request + response).
 *
 * @param <E>
 *            source type (e.g. {@code Product})
 * @param <D>
 *            API DTO type (e.g. {@code ProductResponse})
 * @see AbstractOutboundMapperImpl
 */
public interface OutboundMapper<E, D> {

	D toDto(@NonNull E entity);

	/**
	 * Maps a list of entities, skipping null elements. Returns an empty list when
	 * {@code entities} is null or empty.
	 */
	default List<D> toDtoList(@Nullable List<E> entities) {
		if (CollectionUtils.isEmpty(entities)) {
			return Collections.emptyList();
		}
		/* spotless:off */
		return entities.stream()
				.filter(Objects::nonNull)
				.map(this::toDto)
				.toList();
		/* spotless:on */
	}
}
