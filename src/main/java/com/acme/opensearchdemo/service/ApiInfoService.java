package com.acme.opensearchdemo.service;

import com.acme.opensearchdemo.model.ApiInfo;

public interface ApiInfoService {

	ApiInfo fromRequest(String requestBaseUrl);

	ApiInfo fromServerPort(int serverPort);
}
