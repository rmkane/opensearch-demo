package com.acme.opensearchdemo.service;

import com.acme.opensearch.model.ApiInfo;

public interface ApiInfoService {

	ApiInfo fromRequest(String requestBaseUrl);

	ApiInfo fromServerPort(int serverPort);
}
