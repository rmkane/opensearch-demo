package com.acme.opensearchdemo.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.opensearchdemo.model.ProductDocument;
import com.acme.opensearchdemo.service.ProductSearchService;

@ExtendWith(MockitoExtension.class)
class ProductSearchControllerTest {

	@Mock
	private ProductSearchService service;

	@InjectMocks
	private ProductSearchController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void findByIdReturnsProduct() throws Exception {
		ProductDocument document = new ProductDocument("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.findById("p-1")).thenReturn(Optional.of(document));

		mockMvc.perform(get("/api/products/p-1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("p-1"))
				.andExpect(jsonPath("$.name").value("Coffee Mug")).andExpect(jsonPath("$.sku").value("MUG-001"))
				.andExpect(jsonPath("$.price").value(12.99));
	}

	@Test
	void findByIdReturns404WhenMissing() throws Exception {
		when(service.findById("missing")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/products/missing")).andExpect(status().isNotFound());
	}

	@Test
	void findAllReturnsProducts() throws Exception {
		ProductDocument document = new ProductDocument("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.findAll()).thenReturn(List.of(document));

		mockMvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("p-1"));
	}

	@Test
	void saveReturnsPersistedProduct() throws Exception {
		ProductDocument saved = new ProductDocument("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.save(any(ProductDocument.class))).thenReturn(saved);

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"id":"p-1","name":"Coffee Mug","sku":"MUG-001","price":12.99}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("p-1"));
	}

	@Test
	void javaClientIndexReturnsProblemDetailOnIOException() throws Exception {
		when(service.createIndexUsingJavaClient()).thenThrow(new IOException("connection refused"));

		mockMvc.perform(post("/api/products/index/java-client")).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502)).andExpect(jsonPath("$.detail").value("connection refused"));
	}

	@Test
	void springDataIndexReturnsResult() throws Exception {
		when(service.createIndexUsingSpringData()).thenReturn(Map.of("created", true, "index", "products"));

		mockMvc.perform(post("/api/products/index/spring-data")).andExpect(status().isOk())
				.andExpect(jsonPath("$.created").value(true)).andExpect(jsonPath("$.index").value("products"));
	}
}
