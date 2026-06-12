package com.acme.opensearchdemo.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.opensearch.model.Product;

import com.acme.opensearchdemo.mapper.ProductResponseMapper;
import com.acme.opensearchdemo.service.ProductSearchService;

@ExtendWith(MockitoExtension.class)
class ProductSearchControllerTest {

	@Mock
	private ProductSearchService service;

	private ProductSearchController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		controller = new ProductSearchController(service, new ProductResponseMapper());
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void findByIdReturnsProduct() throws Exception {
		Product document = new Product("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.findById("p-1")).thenReturn(Optional.of(document));

		mockMvc.perform(get("/api/products/p-1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("p-1")).andExpect(jsonPath("$.name").value("Coffee Mug"))
				.andExpect(jsonPath("$.sku").value("MUG-001")).andExpect(jsonPath("$.price").value(12.99));
	}

	@Test
	void findByIdReturns404WhenMissing() throws Exception {
		when(service.findById("missing")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/products/missing").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}

	@Test
	void findAllReturnsProducts() throws Exception {
		Product document = new Product("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.findAll()).thenReturn(List.of(document));

		mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("p-1"));
	}

	@Test
	void replaceReturnsUpdatedProduct() throws Exception {
		Product replaced = new Product("p-1", "Large Mug", "MUG-001", new BigDecimal("14.99"), null);
		when(service.replace(eq("p-1"), any(Product.class))).thenReturn(Optional.of(replaced));

		mockMvc.perform(put("/api/products/p-1").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content("""
						{"name":"Large Mug","sku":"MUG-001","price":14.99}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Large Mug"));
	}

	@Test
	void replaceReturns404WhenMissing() throws Exception {
		when(service.replace(eq("missing"), any(Product.class))).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/products/missing").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content("""
				{"name":"Large Mug","sku":"MUG-001","price":14.99}
				""")).andExpect(status().isNotFound());
	}

	@Test
	void replaceReturns400WhenIdMismatches() throws Exception {
		when(service.replace(eq("p-1"), any(Product.class)))
				.thenThrow(new IllegalArgumentException("ID in body does not match path"));

		mockMvc.perform(put("/api/products/p-1").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content("""
				{"id":"p-2","name":"Large Mug","sku":"MUG-001","price":14.99}
				""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("ID in body does not match path"));
	}

	@Test
	void patchReturnsUpdatedProduct() throws Exception {
		Product patched = new Product("p-1", "Coffee Mug", "MUG-001", new BigDecimal("9.99"), null);
		when(service.update(eq("p-1"), any(Product.class))).thenReturn(Optional.of(patched));

		mockMvc.perform(patch("/api/products/p-1").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content("""
						{"price":9.99}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(9.99));
	}

	@Test
	void patchReturns404WhenMissing() throws Exception {
		when(service.update(eq("missing"), any(Product.class))).thenReturn(Optional.empty());

		mockMvc.perform(patch("/api/products/missing").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content("""
				{"price":9.99}
				""")).andExpect(status().isNotFound());
	}

	@Test
	void deleteReturns204WhenPresent() throws Exception {
		when(service.deleteById("p-1")).thenReturn(true);

		mockMvc.perform(delete("/api/products/p-1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
	}

	@Test
	void deleteReturns404WhenMissing() throws Exception {
		when(service.deleteById("missing")).thenReturn(false);

		mockMvc.perform(delete("/api/products/missing").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void purgeAllWithoutQueryParamReturnsBadRequest() throws Exception {
		mockMvc.perform(delete("/api/products").accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void purgeAllReturnsDeletedCount() throws Exception {
		when(service.purgeAll()).thenReturn(2L);

		mockMvc.perform(delete("/api/products").param("purge", "true").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.purged").value(true)).andExpect(jsonPath("$.deleted").value(2))
				.andExpect(jsonPath("$.index").value("products"));
	}

	@Test
	void saveReturnsPersistedProduct() throws Exception {
		Product saved = new Product("p-1", "Coffee Mug", "MUG-001", new BigDecimal("12.99"), null);
		when(service.save(any(Product.class))).thenReturn(saved);

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.content("""
						{"id":"p-1","name":"Coffee Mug","sku":"MUG-001","price":12.99}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("p-1"));
	}

	@Test
	void javaClientIndexReturnsProblemDetailOnIOException() throws Exception {
		when(service.createIndexUsingJavaClient()).thenThrow(new IOException("connection refused"));

		mockMvc.perform(post("/api/products/index/java-client").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502)).andExpect(jsonPath("$.detail").value("connection refused"));
	}

	@Test
	void springDataIndexReturnsResult() throws Exception {
		when(service.createIndexUsingSpringData()).thenReturn(Map.of("created", true, "index", "products"));

		mockMvc.perform(post("/api/products/index/spring-data").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.created").value(true)).andExpect(jsonPath("$.index").value("products"));
	}
}
