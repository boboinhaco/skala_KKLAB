package com.sk.skala.shopapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sk.skala.shopapi.common.Response;

import lombok.extern.slf4j.Slf4j;

// 컨트롤러 전역에서 발생한 예외를 공통 응답 형태로 변환
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseException.class)
	public ResponseEntity<Response> handleResponseException(ResponseException e) {
		log.warn("ResponseException: {}", e.getMessage());
		return ResponseEntity.status(e.getError().getStatus())
				.body(Response.error(e.getError().name(), e.getMessage()));
	}

	@ExceptionHandler(ParameterException.class)
	public ResponseEntity<Response> handleParameterException(ParameterException e) {
		log.warn("ParameterException: {}", e.getMessage());
		return ResponseEntity.badRequest()
				.body(Response.error("INVALID_PARAMETER", e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response> handleException(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Response.error(Error.SYSTEM_ERROR.name(), Error.SYSTEM_ERROR.getMessage()));
	}
}
