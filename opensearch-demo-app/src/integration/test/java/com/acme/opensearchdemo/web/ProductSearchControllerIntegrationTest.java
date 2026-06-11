package com.acme.opensearchdemo.web;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.acme.common.BaseIntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.opensearchdemo.model.ProductDocument;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSearchControllerIntegrationTest extends BaseIntegrationTest {

	private static final String BASE_URL = "/api/products";

	@Test
	@Order(1)
	void shouldCreateIndex() {
		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL + "/index/java-client"), HttpMethod.PUT,
				acceptJsonEntity(), String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().contains("\"index\":\"products\""));
		assertTrue(response.getBody().contains("\"created\":true"));
		captureResponse(response.getBody(), "shouldCreateIndex.json");
	}

	@Test
	@Order(2)
	void shouldCreateProduct() {
		ProductDocument product = new ProductDocument("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL), HttpMethod.POST, jsonEntity(product),
				String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		captureResponse(response.getBody(), "shouldCreateProduct.json");
	}

	@Test
	@Order(3)
	void shouldReturnProductById() {
		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL + "/{id}"), HttpMethod.GET,
				acceptJsonEntity(), String.class, "p-1");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		captureResponse(response.getBody(), "shouldReturnProductById.json");
	}

	@Test
	@Order(4)
	void shouldReturnAllProducts() {
		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL), HttpMethod.GET, acceptJsonEntity(),
				String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		captureResponse(response.getBody(), "shouldReturnAllProducts.json");
	}

	@Test
	@Order(5)
	void shouldUpdateProduct() {
		ProductDocument product = new ProductDocument(null, "Large Coffee Mug", "MUG-001", new BigDecimal("14.99"),
				null);
		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL + "/{id}"), HttpMethod.PUT,
				jsonEntity(product), String.class, "p-1");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		captureResponse(response.getBody(), "shouldUpdateProduct.json");
	}

	@Test
	@Order(6)
	void shouldDeleteProduct() {
		ResponseEntity<Void> response = restTemplate.exchange(apiUrl(BASE_URL + "/{id}"), HttpMethod.DELETE,
				acceptJsonEntity(), Void.class, "p-1");
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	@Order(7)
	void shouldPurgeAllProducts() throws JsonProcessingException {
		ProductDocument product = new ProductDocument("p-purge", "Purge Test", "PURGE-001", new BigDecimal("1.00"),
				null);
		restTemplate.exchange(apiUrl(BASE_URL), HttpMethod.POST, jsonEntity(product), String.class);

		ResponseEntity<String> response = restTemplate.exchange(apiUrl(BASE_URL + "?purge=true"), HttpMethod.DELETE,
				acceptJsonEntity(), String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		Map<String, Object> body = parseBody(response.getBody());
		assertTrue(Boolean.parseBoolean(body.get("purged").toString()));
		assertEquals("products", body.get("index"));
		assertEquals(1L, Integer.parseInt(body.get("deleted").toString()));

		captureResponse(response.getBody(), "shouldPurgeAllProducts.json");

		ResponseEntity<String> list = restTemplate.exchange(apiUrl(BASE_URL), HttpMethod.GET, acceptJsonEntity(),
				String.class);
		assertEquals(HttpStatus.OK, list.getStatusCode());
		assertEquals("[]", list.getBody());
	}
}
