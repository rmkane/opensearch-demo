package com.acme.opensearchdemo.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.acme.opensearch.model.ProductsIndex;

import com.acme.opensearchdemo.dto.ProductResponse;
import com.acme.opensearchdemo.mapper.ProductResponseMapper;
import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.service.ProductSearchService;

/**
 * Demo endpoints comparing Spring Data OpenSearch and the direct
 * {@code opensearch-java} client.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

	private final ProductSearchService service;
	private final ProductResponseMapper productResponseMapper;

	// --- Spring Data OpenSearch (IndexOperations) ---

	@PostMapping("/index/spring-data")
	public ResponseEntity<Map<String, Object>> createIndexUsingSpringData() {
		return ResponseEntity.ok(service.createIndexUsingSpringData());
	}

	@PutMapping("/index/spring-data")
	public ResponseEntity<Map<String, Object>> recreateIndexUsingSpringData() {
		return ResponseEntity.ok(service.recreateIndexUsingSpringData());
	}

	// --- opensearch-java client ---

	@PostMapping("/index/java-client")
	public ResponseEntity<Map<String, Object>> createIndexUsingJavaClient() throws IOException {
		return ResponseEntity.ok(service.createIndexUsingJavaClient());
	}

	@PutMapping("/index/java-client")
	public ResponseEntity<Map<String, Object>> recreateIndexUsingJavaClient() throws IOException {
		return ResponseEntity.ok(service.recreateIndexUsingJavaClient());
	}

	// OpenSearch allows adding new mapping fields; changing an existing field type
	// requires reindex.
	@PutMapping("/index/java-client/mapping/description")
	public ResponseEntity<Map<String, Object>> addDescriptionFieldUsingJavaClient() throws IOException {
		return ResponseEntity.ok(service.addDescriptionFieldUsingJavaClient());
	}

	@PutMapping("/index/java-client/settings/refresh-interval/{refreshInterval}")
	public ResponseEntity<Map<String, Object>> updateRefreshIntervalUsingJavaClient(
			@PathVariable String refreshInterval) throws IOException {
		return ResponseEntity.ok(service.updateRefreshIntervalUsingJavaClient(refreshInterval));
	}

	// --- Spring Data repository ---

	@PostMapping
	public ResponseEntity<ProductResponse> save(@RequestBody ProductDocument document) {
		return ResponseEntity.ok(productResponseMapper.toDto(service.save(document)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductResponse> replace(@PathVariable String id, @RequestBody ProductDocument document) {
		/* spotless:off */
		return service.replace(id, document)
				.map(productResponseMapper::toDto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
		/* spotless:on */
	}

	@PatchMapping("/{id}")
	public ResponseEntity<ProductResponse> update(@PathVariable String id, @RequestBody ProductDocument patch) {
		/* spotless:off */
		return service.update(id, patch)
				.map(productResponseMapper::toDto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
		/* spotless:on */
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		/* spotless:off */
		return service.deleteById(id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
		/* spotless:on */
	}

	/**
	 * Deletes every document in the index; mapping and settings are left intact.
	 */
	@DeleteMapping(params = "purge")
	public ResponseEntity<Map<String, Object>> purgeAll() {
		long deleted = service.purgeAll();
		/* spotless:off */
		return ResponseEntity.ok(Map.ofEntries(
			Map.entry("purged", true),
			Map.entry("index", ProductsIndex.INDEX_NAME),
			Map.entry("deleted", deleted)
		));
		/* spotless:on */
	}

	@GetMapping
	public ResponseEntity<List<ProductResponse>> findAll() {
		return ResponseEntity.ok(productResponseMapper.toDtoList(service.findAll()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> findById(@PathVariable String id) {
		/* spotless:off */
		return service.findById(id)
				.map(productResponseMapper::toDto)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
		/* spotless:on */
	}
}
