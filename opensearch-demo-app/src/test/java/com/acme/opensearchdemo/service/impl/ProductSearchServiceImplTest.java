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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.opensearch.util.ProductIndexOperations;

import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.repository.ProductRepository;

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
		when(repository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductDocument saved = service.save(new ProductDocument("p-1", "Coffee Mug", "MUG-001", null, null));

		assertThat(saved.price()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(saved.updatedOn()).isNotNull();
		verify(repository).save(any(ProductDocument.class));
	}

	@Test
	void findByIdDelegatesToRepository() {
		ProductDocument document = new ProductDocument("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null);
		when(repository.findById("p-1")).thenReturn(Optional.of(document));

		assertThat(service.findById("p-1")).contains(document);
	}

	@Test
	void findAllDelegatesToRepository() {
		List<ProductDocument> documents = List
				.of(new ProductDocument("p-1", "Coffee Mug", "MUG-001", BigDecimal.TEN, null));
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
