package com.acme.opensearchdemo.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.acme.opensearch.model.ApiInfo;

import com.acme.opensearchdemo.service.ApiInfoService;

@Service
public class ApiInfoServiceImpl implements ApiInfoService {

	private static final String PRODUCTS_API_PATH = "/api/products";

	private final String applicationName;
	private final String configuredBaseUrl;
	private final String contextPath;
	private final String swaggerUiPath;
	private final String openApiPath;
	private final String actuatorBasePath;

	public ApiInfoServiceImpl(@Value("${spring.application.name}") String applicationName,
			@Value("${app.base-url:}") String configuredBaseUrl,
			@Value("${server.servlet.context-path:}") String contextPath,
			@Value("${springdoc.swagger-ui.path:/swagger-ui.html}") String swaggerUiPath,
			@Value("${springdoc.api-docs.path:/v3/api-docs}") String openApiPath,
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		this.applicationName = applicationName;
		this.configuredBaseUrl = configuredBaseUrl;
		this.contextPath = normalizeContextPath(contextPath);
		this.swaggerUiPath = normalizePath(swaggerUiPath);
		this.openApiPath = normalizePath(openApiPath);
		this.actuatorBasePath = normalizePath(actuatorBasePath);
	}

	@Override
	public ApiInfo fromRequest(String requestBaseUrl) {
		return createApiInfo(resolveBaseUrl(requestBaseUrl));
	}

	@Override
	public ApiInfo fromServerPort(int serverPort) {
		return createApiInfo(defaultLocalBaseUrl(serverPort));
	}

	private ApiInfo createApiInfo(String baseUrl) {
		String normalizedBase = stripTrailingSlash(baseUrl);

		return new ApiInfo(applicationName, normalizedBase,
				new ApiInfo.Documentation(normalizedBase + swaggerUiPath, normalizedBase + openApiPath),
				new ApiInfo.Actuator(normalizedBase + actuatorBasePath + "/health"),
				new ApiInfo.Api(normalizedBase + PRODUCTS_API_PATH));
	}

	private String resolveBaseUrl(String requestBaseUrl) {
		if (StringUtils.hasText(configuredBaseUrl)) {
			return stripTrailingSlash(configuredBaseUrl.trim());
		}
		if (StringUtils.hasText(requestBaseUrl)) {
			return stripTrailingSlash(requestBaseUrl.trim());
		}
		throw new IllegalStateException("Unable to resolve API base URL");
	}

	private String defaultLocalBaseUrl(int serverPort) {
		if (StringUtils.hasText(configuredBaseUrl)) {
			return stripTrailingSlash(configuredBaseUrl.trim());
		}
		return "http://localhost:" + serverPort + contextPath;
	}

	private String normalizeContextPath(String path) {
		if (!StringUtils.hasText(path) || "/".equals(path)) {
			return "";
		}
		return normalizePath(path);
	}

	private String normalizePath(String path) {
		if (!path.startsWith("/")) {
			return "/" + path;
		}
		return path;
	}

	private String stripTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
