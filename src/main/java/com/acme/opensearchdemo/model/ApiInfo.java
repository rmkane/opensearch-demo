package com.acme.opensearchdemo.model;

public record ApiInfo(String application, String baseUrl, Documentation documentation, Actuator actuator, Api api) {
	public record Documentation(String swaggerUi, String openApi) {
	}

	public record Actuator(String health) {
	}

	public record Api(String products) {
	}
}
