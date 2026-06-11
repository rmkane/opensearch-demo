package com.acme.opensearchdemo.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Hidden;

import com.acme.opensearch.model.ApiInfo;

import com.acme.opensearchdemo.service.ApiInfoService;

@Hidden
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeRoute {

	private final ApiInfoService apiInfoService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiInfo home() {
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return apiInfoService.fromRequest(baseUrl);
	}
}
