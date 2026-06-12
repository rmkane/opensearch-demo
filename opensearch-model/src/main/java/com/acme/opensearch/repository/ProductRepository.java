package com.acme.opensearch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.acme.opensearch.model.Product;

public interface ProductRepository extends ElasticsearchRepository<Product, String> {
}
