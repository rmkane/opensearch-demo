package com.acme.opensearchdemo.service;

import java.io.IOException;
import java.util.List;
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

	Optional<ProductDocument> replace(String id, ProductDocument document);

	Optional<ProductDocument> update(String id, ProductDocument patch);

	boolean deleteById(String id);

	long purgeAll();

	List<ProductDocument> findAll();

	Optional<ProductDocument> findById(String id);
}
