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

import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.repository.ProductRepository;
import com.acme.opensearchdemo.service.ProductSearchService;

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
			return Map.of("created", false, "index", INDEX, "message", "Index already exists");
		}

		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		return Map.of("created", true, "index", INDEX, "client", "spring-data-opensearch");
	}

	@Override
	public Map<String, Object> recreateIndexUsingSpringData() {
		IndexOperations index = operations.indexOps(ProductDocument.class);

		if (index.exists()) {
			index.delete();
		}

		index.create();
		index.putMapping(index.createMapping(ProductDocument.class));

		return Map.of("recreated", true, "index", INDEX, "client", "spring-data-opensearch");
	}

	@Override
	public Map<String, Object> createIndexUsingJavaClient() throws IOException {
		if (indexExists()) {
			return Map.of("created", false, "index", INDEX, "message", "Index already exists");
		}

		CreateIndexResponse response = client.indices()
				.create(request -> request.index(INDEX)
						.settings(settings -> settings.numberOfShards(1).numberOfReplicas(0))
						.mappings(mapping -> mapping.dynamic(DynamicMapping.Strict)
								.properties("id", property -> property.keyword(keyword -> keyword))
								.properties("name", property -> property.text(text -> text))
								.properties("sku", property -> property.keyword(keyword -> keyword))
								.properties("price", property -> property.double_(number -> number))
								.properties("updatedOn", property -> property.date(date -> date))));

		return Map.of("created", response.acknowledged(), "index", INDEX, "client", "opensearch-java");
	}

	@Override
	public Map<String, Object> recreateIndexUsingJavaClient() throws IOException {
		if (indexExists()) {
			DeleteIndexResponse deleteResponse = client.indices().delete(request -> request.index(INDEX));
			if (!deleteResponse.acknowledged()) {
				return Map.of("recreated", false, "index", INDEX, "message", "Delete was not acknowledged");
			}
		}

		return createIndexUsingJavaClient();
	}

	@Override
	public Map<String, Object> addDescriptionFieldUsingJavaClient() throws IOException {
		PutMappingResponse response = client.indices().putMapping(
				request -> request.index(INDEX).properties("description", property -> property.text(text -> text)));

		return Map.of("acknowledged", response.acknowledged(), "index", INDEX, "addedField", "description");
	}

	@Override
	public Map<String, Object> updateRefreshIntervalUsingJavaClient(String refreshInterval) throws IOException {
		PutIndicesSettingsResponse response = client.indices().putSettings(request -> request.index(INDEX)
				.settings(settings -> settings.refreshInterval(Time.of(t -> t.time(refreshInterval)))));

		return Map.of("acknowledged", response.acknowledged(), "index", INDEX, "refreshInterval", refreshInterval);
	}

	@Override
	public ProductDocument save(ProductDocument document) {
		ProductDocument normalized = new ProductDocument(document.id(), document.name(), document.sku(),
				document.price() == null ? BigDecimal.ZERO : document.price(), Instant.now());

		return repository.save(normalized);
	}

	@Override
	public Iterable<ProductDocument> findAll() {
		return repository.findAll();
	}

	private boolean indexExists() throws IOException {
		ExistsRequest request = ExistsRequest.of(builder -> builder.index(INDEX));
		return client.indices().exists(request).value();
	}
}
