package com.acme.opensearch.util;

import java.io.IOException;
import java.util.Map;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.PutMappingResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acme.opensearch.model.Product;

/**
 * Index lifecycle operations via {@code opensearch-java} (no Spring
 * dependencies).
 * <p>
 * Used by the demo app today; intended to move into a dedicated java-client
 * starter later.
 */
@Slf4j
@RequiredArgsConstructor
public class ProductIndexOperations {

	private final OpenSearchClient client;

	public Map<String, Object> createIfAbsent() throws IOException {
		if (indexExists()) {
			log.warn("Index '{}' already exists; skipping opensearch-java create", Product.INDEX_NAME);
			/* spotless:off */
			return Map.ofEntries(
				Map.entry("created", false),
				Map.entry("index", Product.INDEX_NAME),
				Map.entry("message", "Index already exists")
			);
			/* spotless:on */
		}

		ProductsIndexSettingsSupport.Values indexSettings = ProductsIndexSettingsSupport.load();
		log.info("Creating index '{}' via opensearch-java client", Product.INDEX_NAME);
		CreateIndexResponse response = client.indices()
				.create(request -> request.index(Product.INDEX_NAME)
						.settings(settings -> settings.numberOfShards(indexSettings.numberOfShards())
								.numberOfReplicas(indexSettings.numberOfReplicas())
								.refreshInterval(Time.of(t -> t.time(indexSettings.refreshInterval()))))
						// Strict dynamic mapping — reject documents with unknown fields.
						.mappings(mapping -> mapping.dynamic(DynamicMapping.Strict)
								.properties("id", property -> property.keyword(keyword -> keyword))
								.properties("name", property -> property.text(text -> text))
								.properties("sku", property -> property.keyword(keyword -> keyword))
								.properties("price", property -> property.double_(number -> number))
								.properties("updatedOn", property -> property.date(date -> date))));

		log.info("Created index '{}' via opensearch-java client (acknowledged={})", Product.INDEX_NAME,
				response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("created", response.acknowledged()),
			Map.entry("index", Product.INDEX_NAME),
			Map.entry("client", "opensearch-java")
		);
		/* spotless:on */
	}

	public Map<String, Object> recreate() throws IOException {
		if (indexExists()) {
			log.info("Deleting index '{}' before opensearch-java recreate", Product.INDEX_NAME);
			DeleteIndexResponse deleteResponse = client.indices().delete(request -> request.index(Product.INDEX_NAME));
			if (!deleteResponse.acknowledged()) {
				log.warn("Delete of index '{}' was not acknowledged", Product.INDEX_NAME);
				/* spotless:off */
				return Map.ofEntries(
					Map.entry("recreated", false),
					Map.entry("index", Product.INDEX_NAME),
					Map.entry("message", "Delete was not acknowledged")
				);
				/* spotless:on */
			}
		}

		return createIfAbsent();
	}

	public Map<String, Object> addDescriptionField() throws IOException {
		log.info("Adding 'description' field to index '{}' mapping", Product.INDEX_NAME);
		PutMappingResponse response = client.indices().putMapping(request -> request.index(Product.INDEX_NAME)
				.properties("description", property -> property.text(text -> text)));

		log.info("Updated index '{}' mapping (acknowledged={})", Product.INDEX_NAME, response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("acknowledged", response.acknowledged()),
			Map.entry("index", Product.INDEX_NAME),
			Map.entry("addedField", "description")
		);
		/* spotless:on */
	}

	public Map<String, Object> updateRefreshInterval(String refreshInterval) throws IOException {
		log.info("Updating refresh_interval for index '{}' to {}", Product.INDEX_NAME, refreshInterval);
		// refreshInterval expects Time.of(t -> t.time("5s")), not a raw string
		// (opensearch-java 3.x).
		PutIndicesSettingsResponse response = client.indices().putSettings(request -> request.index(Product.INDEX_NAME)
				.settings(settings -> settings.refreshInterval(Time.of(t -> t.time(refreshInterval)))));

		log.info("Updated index '{}' settings (acknowledged={})", Product.INDEX_NAME, response.acknowledged());
		/* spotless:off */
		return Map.ofEntries(
			Map.entry("acknowledged", response.acknowledged()),
			Map.entry("index", Product.INDEX_NAME),
			Map.entry("refreshInterval", refreshInterval)
		);
		/* spotless:on */
	}

	private boolean indexExists() throws IOException {
		ExistsRequest request = ExistsRequest.of(builder -> builder.index(Product.INDEX_NAME));
		return client.indices().exists(request).value();
	}
}
