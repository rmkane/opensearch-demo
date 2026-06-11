package com.acme.opensearchdemo.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.acme.opensearchdemo.model.ProductDocument;

public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {
}
