package com.acme.opensearchdemo.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.acme.opensearchdemo.model.ApiInfo;
import com.acme.opensearchdemo.service.ApiInfoService;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

	private final ApiInfoService apiInfoService;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		int port = resolveServerPort(event);
		ApiInfo info = apiInfoService.fromServerPort(port);

		log.info("{} is ready", info.application());
		log.info("  Home:         {}", info.baseUrl());
		log.info("  Swagger UI:   {}", info.documentation().swaggerUi());
		log.info("  OpenAPI:      {}", info.documentation().openApi());
		log.info("  Health:       {}", info.actuator().health());
		log.info("  Products API: {}", info.api().products());
	}

	private int resolveServerPort(ApplicationReadyEvent event) {
		if (event.getApplicationContext() instanceof WebServerApplicationContext webContext) {
			return webContext.getWebServer().getPort();
		}
		// Fallback when running without an embedded web server (e.g. tests).
		String port = event.getApplicationContext().getEnvironment().getProperty("local.server.port", "8080");
		return Integer.parseInt(port);
	}
}
