package com.acme.opensearchdemo.web;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Test
	void handleMissingRequestParameter() {
		ProblemDetail problem = handler.handleMissingRequestParameter(
				new UnsatisfiedServletRequestParameterException(new String[]{"purge"}, Map.of()));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(problem.getDetail()).contains("purge");
	}

	@Test
	void handleBadRequest() {
		ProblemDetail problem = handler.handleBadRequest(new IllegalArgumentException("invalid id"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(problem.getDetail()).isEqualTo("invalid id");
	}

	@Test
	void handleIOException() {
		ProblemDetail problem = handler.handleIOException(new IOException("connection refused"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
		assertThat(problem.getDetail()).isEqualTo("connection refused");
	}

	@Test
	void handleIllegalState() {
		ProblemDetail problem = handler.handleIllegalState(new IllegalStateException("misconfigured"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problem.getDetail()).isEqualTo("misconfigured");
	}

	@Test
	void handleUnexpected() {
		ProblemDetail problem = handler.handleUnexpected(new RuntimeException("secret internals"));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
	}
}
