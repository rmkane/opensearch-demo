package com.acme.opensearchdemo.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.acme.opensearch.model.Product;

public interface ProductSearchService {
	Map<String, Object> createIndexUsingSpringData();

	Map<String, Object> recreateIndexUsingSpringData();

	Map<String, Object> createIndexUsingJavaClient() throws IOException;

	Map<String, Object> recreateIndexUsingJavaClient() throws IOException;

	Map<String, Object> addDescriptionFieldUsingJavaClient() throws IOException;

	Map<String, Object> updateRefreshIntervalUsingJavaClient(String refreshInterval) throws IOException;

	Product save(Product document);

	Optional<Product> replace(String id, Product document);

	Optional<Product> update(String id, Product patch);

	boolean deleteById(String id);

	long purgeAll();

	List<Product> findAll();

	Optional<Product> findById(String id);
}
