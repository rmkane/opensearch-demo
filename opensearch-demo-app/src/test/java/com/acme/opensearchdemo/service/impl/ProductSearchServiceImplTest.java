package com.acme.opensearchdemo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.opensearch.model.Product;
import com.acme.opensearch.repository.ProductRepository;
import com.acme.opensearch.util.ProductIndexOperations;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

	@Mock
	private ProductRepository repository;

	@Mock
	private ElasticsearchOperations operations;

	@Mock
	private ProductIndexOperations productIndexOperations;

	@InjectMocks
	private ProductSearchServiceImpl service;

	@Test
	void saveNormalizesNullPriceAndSetsUpdatedOn() {
		when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Product saved = service.save(new Product("p-1", "Coffee Mug", "MUG-001", null, null));

		assertThat(saved.price()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(saved.updatedOn()).isNotNull();
		verify(repository).save(any(Product.class));
	}

	@Test
	void replaceUpdatesExistingProduct() {
		Product existing = new Product("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null);
		when(repository.existsById("p-1")).thenReturn(true);
		when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<Product> replaced = service.replace("p-1",
				new Product(null, "Large Mug", "MUG-001", new BigDecimal("14.99"), null));

		assertThat(replaced).isPresent();
		assertThat(replaced.orElseThrow().id()).isEqualTo("p-1");
		assertThat(replaced.orElseThrow().name()).isEqualTo("Large Mug");
		verify(repository).save(any(Product.class));
	}

	@Test
	void replaceReturnsEmptyWhenMissing() {
		when(repository.existsById("missing")).thenReturn(false);

		assertThat(service.replace("missing", new Product(null, "Large Mug", "MUG-001", BigDecimal.ONE, null)))
				.isEmpty();
		verify(repository, never()).save(any());
	}

	@Test
	void replaceRejectsMismatchedId() {
		assertThatThrownBy(
				() -> service.replace("p-1", new Product("p-2", "Large Mug", "MUG-001", BigDecimal.ONE, null)))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("ID in body does not match path");
	}

	@Test
	void updatePatchesExistingFields() {
		Product existing = new Product("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null);
		when(repository.findById("p-1")).thenReturn(Optional.of(existing));
		when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<Product> updated = service.update("p-1", new Product(null, null, null, new BigDecimal("9.99"), null));

		assertThat(updated).isPresent();
		assertThat(updated.orElseThrow().name()).isEqualTo("Coffee Mug");
		assertThat(updated.orElseThrow().price()).isEqualByComparingTo("9.99");
	}

	@Test
	void updateReturnsEmptyWhenMissing() {
		when(repository.findById("missing")).thenReturn(Optional.empty());

		assertThat(service.update("missing", new Product(null, null, null, BigDecimal.ONE, null))).isEmpty();
	}

	@Test
	void deleteByIdRemovesExistingProduct() {
		when(repository.existsById("p-1")).thenReturn(true);

		assertThat(service.deleteById("p-1")).isTrue();
		verify(repository).deleteById("p-1");
	}

	@Test
	void deleteByIdReturnsFalseWhenMissing() {
		when(repository.existsById("missing")).thenReturn(false);

		assertThat(service.deleteById("missing")).isFalse();
		verify(repository, never()).deleteById(any());
	}

	@Test
	void purgeAllDeletesEveryDocument() {
		when(repository.count()).thenReturn(3L);

		assertThat(service.purgeAll()).isEqualTo(3L);
		verify(repository).deleteAll();
	}

	@Test
	void findByIdDelegatesToRepository() {
		Product document = new Product("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null);
		when(repository.findById("p-1")).thenReturn(Optional.of(document));

		assertThat(service.findById("p-1")).contains(document);
	}

	@Test
	void findAllDelegatesToRepository() {
		List<Product> documents = List.of(new Product("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null));
		when(repository.findAll()).thenReturn(documents);

		assertThat(service.findAll()).containsExactlyElementsOf(documents);
	}

	@Test
	void createIndexUsingJavaClientDelegatesToUtil() throws IOException {
		Map<String, Object> expected = Map.of("created", true, "index", "products");
		when(productIndexOperations.createIfAbsent()).thenReturn(expected);

		assertThat(service.createIndexUsingJavaClient()).isEqualTo(expected);
	}
}
