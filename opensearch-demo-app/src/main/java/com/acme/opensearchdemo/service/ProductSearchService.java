package com.acme.opensearchdemo.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.acme.opensearchdemo.model.ProductDocument;

public interface ProductSearchService {
	Map<String, Object> createIndexUsingSpringData();
	Map<String, Object> recreateIndexUsingSpringData();
	Map<String, Object> createIndexUsingJavaClient() throws IOException;
	Map<String, Object> recreateIndexUsingJavaClient() throws IOException;
	Map<String, Object> addDescriptionFieldUsingJavaClient() throws IOException;
	Map<String, Object> updateRefreshIntervalUsingJavaClient(String refreshInterval) throws IOException;
	ProductDocument save(ProductDocument document);
	Iterable<ProductDocument> findAll();
	Optional<ProductDocument> findById(String id);
}
