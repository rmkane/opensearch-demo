package com.acme.opensearchdemo.web;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

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
	public ResponseEntity<ProductDocument> save(@RequestBody ProductDocument document) {
		return ResponseEntity.ok(service.save(document));
	}

	@GetMapping
	public ResponseEntity<Iterable<ProductDocument>> findAll() {
		return ResponseEntity.ok(service.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductDocument> findById(@PathVariable String id) {
		return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}
}
