package com.acme.opensearchdemo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acme.opensearch.model.ProductsIndex;
import com.acme.opensearch.util.ProductIndexOperations;

import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.repository.ProductRepository;
import com.acme.opensearchdemo.service.ProductSearchService;

/**
 * Orchestrates two OpenSearch client paths side by side for comparison:
 * <ul>
 * <li>Spring Data — {@link IndexOperations} + {@link ProductRepository}</li>
 * <li>{@code opensearch-java} — delegated to {@link ProductIndexOperations} in
 * {@code opensearch-util}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

	private final ProductRepository repository;
	private final ElasticsearchOperations operations;
	private final ProductIndexOperations productIndexOperations;

	@Override
	public Map<String, Object> createIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(ProductDocument.class);

		if (index.exists()) {
			log.warn("Index '{}' already exists; skipping Spring Data create", ProductsIndex.INDEX_NAME);
			/* spotless:off */
			return Map.ofEntries(
				Map.entry("created", false),
				Map.entry("index", ProductsIndex.INDEX_NAME),
				Map.entry("message", "Index already exists")
			);
			/* spotless:on */
		}

		log.info("Creating index '{}' via Spring Data OpenSearch", ProductsIndex.INDEX_NAME);
		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		log.info("Created index '{}' via Spring Data OpenSearch", ProductsIndex.INDEX_NAME);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("created", true),
			Map.entry("index", ProductsIndex.INDEX_NAME),
			Map.entry("client", "spring-data-opensearch")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> recreateIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(ProductDocument.class);

		if (index.exists()) {
			log.info("Deleting index '{}' before Spring Data recreate", ProductsIndex.INDEX_NAME);
			index.delete();
		}

		log.info("Recreating index '{}' via Spring Data OpenSearch", ProductsIndex.INDEX_NAME);
		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		log.info("Recreated index '{}' via Spring Data OpenSearch", ProductsIndex.INDEX_NAME);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("recreated", true),
			Map.entry("index", ProductsIndex.INDEX_NAME),
			Map.entry("client", "spring-data-opensearch")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> createIndexUsingJavaClient() throws IOException {
		return productIndexOperations.createIfAbsent();
	}

	@Override
	public Map<String, Object> recreateIndexUsingJavaClient() throws IOException {
		return productIndexOperations.recreate();
	}

	@Override
	public Map<String, Object> addDescriptionFieldUsingJavaClient() throws IOException {
		return productIndexOperations.addDescriptionField();
	}

	@Override
	public Map<String, Object> updateRefreshIntervalUsingJavaClient(String refreshInterval) throws IOException {
		return productIndexOperations.updateRefreshInterval(refreshInterval);
	}

	@Override
	public ProductDocument save(ProductDocument document) {
		ProductDocument normalized = new ProductDocument(document.id(), document.name(), document.sku(),
				document.price() == null ? BigDecimal.ZERO : document.price(), Instant.now());

		ProductDocument saved = repository.save(normalized);
		log.info("Saved product id={} sku={} to index '{}'", saved.id(), saved.sku(), ProductsIndex.INDEX_NAME);
		return saved;
	}

	@Override
	public Iterable<ProductDocument> findAll() {
		log.info("Listing all products from index '{}'", ProductsIndex.INDEX_NAME);
		return repository.findAll();
	}

	@Override
	public Optional<ProductDocument> findById(String id) {
		log.info("Fetching product id={} from index '{}'", id, ProductsIndex.INDEX_NAME);
		return repository.findById(id);
	}
}
