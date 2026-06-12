package com.acme.opensearchdemo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acme.opensearch.model.Product;
import com.acme.opensearch.repository.ProductRepository;
import com.acme.opensearch.util.ProductIndexOperations;

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
		IndexOperations index = operations.indexOps(Product.class);

		if (index.exists()) {
			log.warn("Index '{}' already exists; skipping Spring Data create", Product.INDEX_NAME);
			/* spotless:off */
			return Map.ofEntries(
				Map.entry("created", false),
				Map.entry("index", Product.INDEX_NAME),
				Map.entry("message", "Index already exists")
			);
			/* spotless:on */
		}

		log.info("Creating index '{}' via Spring Data OpenSearch", Product.INDEX_NAME);
		index.createWithMapping();

		log.info("Created index '{}' via Spring Data OpenSearch", Product.INDEX_NAME);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("created", true),
			Map.entry("index", Product.INDEX_NAME),
			Map.entry("client", "spring-data-opensearch")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> recreateIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(Product.class);

		if (index.exists()) {
			log.info("Deleting index '{}' before Spring Data recreate", Product.INDEX_NAME);
			index.delete();
		}

		log.info("Recreating index '{}' via Spring Data OpenSearch", Product.INDEX_NAME);
		index.createWithMapping();

		log.info("Recreated index '{}' via Spring Data OpenSearch", Product.INDEX_NAME);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("recreated", true),
			Map.entry("index", Product.INDEX_NAME),
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
	public Product save(Product document) {
		Product normalized = normalize(document);
		Product saved = repository.save(normalized);
		log.info("Saved product id={} sku={} to index '{}'", saved.id(), saved.sku(), Product.INDEX_NAME);
		return saved;
	}

	@Override
	public Optional<Product> replace(String id, Product document) {
		if (document.id() != null && !document.id().equals(id)) {
			throw new IllegalArgumentException("ID in body does not match path");
		}
		if (!repository.existsById(id)) {
			return Optional.empty();
		}
		Product replaced = normalize(
				new Product(id, document.name(), document.sku(), document.price(), document.updatedOn()));
		Product saved = repository.save(replaced);
		log.info("Replaced product id={} in index '{}'", saved.id(), Product.INDEX_NAME);
		return Optional.of(saved);
	}

	@Override
	public Optional<Product> update(String id, Product patch) {
		return repository.findById(id).map(existing -> {
			Product updated = normalize(new Product(id, patch.name() != null ? patch.name() : existing.name(),
					patch.sku() != null ? patch.sku() : existing.sku(),
					patch.price() != null ? patch.price() : existing.price(), existing.updatedOn()));
			Product saved = repository.save(updated);
			log.info("Updated product id={} in index '{}'", saved.id(), Product.INDEX_NAME);
			return saved;
		});
	}

	@Override
	public boolean deleteById(String id) {
		if (!repository.existsById(id)) {
			return false;
		}
		repository.deleteById(id);
		log.info("Deleted product id={} from index '{}'", id, Product.INDEX_NAME);
		return true;
	}

	@Override
	public long purgeAll() {
		long count = repository.count();
		repository.deleteAll();
		log.warn("Purged {} product(s) from index '{}'", count, Product.INDEX_NAME);
		return count;
	}

	private Product normalize(Product document) {
		return new Product(document.id(), document.name(), document.sku(),
				document.price() == null ? BigDecimal.ZERO : document.price(), Instant.now());
	}

	@Override
	public List<Product> findAll() {
		log.info("Listing all products from index '{}'", Product.INDEX_NAME);
		return StreamSupport.stream(repository.findAll().spliterator(), false).toList();
	}

	@Override
	public Optional<Product> findById(String id) {
		log.info("Fetching product id={} from index '{}'", id, Product.INDEX_NAME);
		return repository.findById(id);
	}
}
