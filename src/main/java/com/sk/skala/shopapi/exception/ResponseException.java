package com.sk.skala.shopapi.exception;

import lombok.Getter;

// 비즈니스 규칙 위반 시 발생시키는 예외
@Getter
public class ResponseException extends RuntimeException {

	private final Error error;

	public ResponseException(Error error) {
		super(error.getMessage());
		this.error = error;
	}

	public ResponseException(Error error, String message) {
		super(message);
		this.error = error;
	}
}
