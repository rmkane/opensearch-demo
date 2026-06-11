package com.acme.opensearchdemo.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps exceptions to RFC 7807 {@link ProblemDetail} responses (Spring Boot 3+).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(IOException.class)
	public ProblemDetail handleIOException(IOException ex) {
		log.error("OpenSearch I/O error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ProblemDetail handleIllegalState(IllegalStateException ex) {
		log.error("Illegal state: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}
}
