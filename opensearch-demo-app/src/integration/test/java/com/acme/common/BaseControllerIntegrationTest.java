package com.acme.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public abstract class BaseControllerIntegrationTest {

	protected static final String SERVER_BASE = System.getenv().getOrDefault("API_BASE", "http://localhost:8080");

	protected final RestTemplate restTemplate = new RestTemplate();

	/* spotless:off */
	protected final ObjectMapper objectMapper = new ObjectMapper()
			.configure(SerializationFeature.INDENT_OUTPUT, true)
			.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	/* spotless:on */

	protected String apiUrl(String path) {
		return SERVER_BASE + path;
	}

	protected String formatJson(String json) {
		try {
			return objectMapper.writeValueAsString(objectMapper.readValue(json, Object.class));
		} catch (IOException e) {
			throw new RuntimeException("Failed to format JSON", e);
		}
	}

	protected void writeResponseToFile(String response, String filePath) {
		try {
			Files.writeString(Path.of(filePath), response);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write response to file", e);
		}
	}

	protected void captureResponse(String body, String fileName) {
		writeResponseToFile(formatJson(body), "target/" + fileName);
	}

	protected HttpEntity<Void> acceptJsonEntity() {
		return new HttpEntity<>(acceptJsonHeaders());
	}

	protected <E> HttpEntity<E> jsonEntity(E entity) {
		return new HttpEntity<>(entity, jsonRequestHeaders());
	}

	protected HttpHeaders acceptJsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		return headers;
	}

	protected HttpHeaders jsonRequestHeaders() {
		HttpHeaders headers = acceptJsonHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}
