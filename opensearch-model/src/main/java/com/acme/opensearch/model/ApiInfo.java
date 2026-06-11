package com.acme.opensearch.model;

/** Links and metadata returned by the demo home route ({@code GET /}). */
public record ApiInfo(String application, String baseUrl, Documentation documentation, Actuator actuator, Api api) {
	public record Documentation(String swaggerUi, String openApi) {
	}

	public record Actuator(String health) {
	}

	public record Api(String products) {
	}
}
