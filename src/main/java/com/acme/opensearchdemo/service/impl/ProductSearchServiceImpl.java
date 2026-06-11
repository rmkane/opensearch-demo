package com.acme.opensearchdemo.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.PutMappingResponse;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.repository.ProductRepository;
import com.acme.opensearchdemo.service.ProductSearchService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

	private static final String INDEX = "products";

	private final ProductRepository repository;
	private final ElasticsearchOperations operations;
	private final OpenSearchClient client;

	@Override
	public Map<String, Object> createIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(ProductDocument.class);

		if (index.exists()) {
			log.warn("Index '{}' already exists; skipping Spring Data create", INDEX);
			/* spotless:off */
			return Map.ofEntries(
				Map.entry("created", false),
				Map.entry("index", INDEX),
				Map.entry("message", "Index already exists")
			);
			/* spotless:on */
		}

		log.info("Creating index '{}' via Spring Data OpenSearch", INDEX);
		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		log.info("Created index '{}' via Spring Data OpenSearch", INDEX);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("created", true),
			Map.entry("index", INDEX),
			Map.entry("client", "spring-data-opensearch")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> recreateIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(ProductDocument.class);

		if (index.exists()) {
			log.info("Deleting index '{}' before Spring Data recreate", INDEX);
			index.delete();
		}

		log.info("Recreating index '{}' via Spring Data OpenSearch", INDEX);
		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		log.info("Recreated index '{}' via Spring Data OpenSearch", INDEX);
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("recreated", true),
			Map.entry("index", INDEX),
			Map.entry("client", "spring-data-opensearch")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> createIndexUsingJavaClient() throws IOException {
		if (indexExists()) {
			log.warn("Index '{}' already exists; skipping opensearch-java create", INDEX);
			/* spotless:off */
			return Map.ofEntries(
				Map.entry("created", false),
				Map.entry("index", INDEX),
				Map.entry("message", "Index already exists")
			);
			/* spotless:on */
		}

		log.info("Creating index '{}' via opensearch-java client", INDEX);
		CreateIndexResponse response = client.indices()
				.create(request -> request.index(INDEX)
						.settings(settings -> settings.numberOfShards(1).numberOfReplicas(0))
						.mappings(mapping -> mapping.dynamic(DynamicMapping.Strict)
								.properties("id", property -> property.keyword(keyword -> keyword))
								.properties("name", property -> property.text(text -> text))
								.properties("sku", property -> property.keyword(keyword -> keyword))
								.properties("price", property -> property.double_(number -> number))
								.properties("updatedOn", property -> property.date(date -> date))));

		log.info("Created index '{}' via opensearch-java client (acknowledged={})", INDEX, response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("created", response.acknowledged()),
			Map.entry("index", INDEX),
			Map.entry("client", "opensearch-java")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> recreateIndexUsingJavaClient() throws IOException {
		if (indexExists()) {
			log.info("Deleting index '{}' before opensearch-java recreate", INDEX);
			DeleteIndexResponse deleteResponse = client.indices().delete(request -> request.index(INDEX));
			if (!deleteResponse.acknowledged()) {
				log.warn("Delete of index '{}' was not acknowledged", INDEX);
				/* spotless:off */
				return Map.ofEntries(
					Map.entry("recreated", false),
					Map.entry("index", INDEX),
					Map.entry("message", "Delete was not acknowledged")
				);
				/* spotless:on */
			}
		}

		return createIndexUsingJavaClient();
	}

	@Override
	public Map<String, Object> addDescriptionFieldUsingJavaClient() throws IOException {
		log.info("Adding 'description' field to index '{}' mapping", INDEX);
		PutMappingResponse response = client.indices().putMapping(
				request -> request.index(INDEX).properties("description", property -> property.text(text -> text)));

		log.info("Updated index '{}' mapping (acknowledged={})", INDEX, response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("acknowledged", response.acknowledged()),
			Map.entry("index", INDEX),
			Map.entry("description", "description")
		);
		/* spotless:on */
	}

	@Override
	public Map<String, Object> updateRefreshIntervalUsingJavaClient(String refreshInterval) throws IOException {
		log.info("Updating refresh_interval for index '{}' to {}", INDEX, refreshInterval);
		PutIndicesSettingsResponse response = client.indices().putSettings(request -> request.index(INDEX)
				.settings(settings -> settings.refreshInterval(Time.of(t -> t.time(refreshInterval)))));

		log.info("Updated index '{}' settings (acknowledged={})", INDEX, response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("acknowledged", response.acknowledged()),
			Map.entry("index", INDEX),
			Map.entry("refreshInterval", refreshInterval)
		);
		/* spotless:on */
	}

	@Override
	public ProductDocument save(ProductDocument document) {
		ProductDocument normalized = new ProductDocument(document.id(), document.name(), document.sku(),
				document.price() == null ? BigDecimal.ZERO : document.price(), Instant.now());

		ProductDocument saved = repository.save(normalized);
		log.info("Saved product id={} sku={} to index '{}'", saved.id(), saved.sku(), INDEX);
		return saved;
	}

	@Override
	public Iterable<ProductDocument> findAll() {
		log.info("Listing all products from index '{}'", INDEX);
		return repository.findAll();
	}

	private boolean indexExists() throws IOException {
		ExistsRequest request = ExistsRequest.of(builder -> builder.index(INDEX));
		return client.indices().exists(request).value();
	}
}
