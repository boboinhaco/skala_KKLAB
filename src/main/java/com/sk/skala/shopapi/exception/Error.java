package com.sk.skala.shopapi.exception;

import org.springframework.http.HttpStatus;

// 서비스 전역에서 사용하는 에러 코드 정의
public enum Error {

	DATA_NOT_FOUND("데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DATA_DUPLICATED("이미 존재하는 데이터입니다.", HttpStatus.CONFLICT),
	NOT_AUTHENTICATED("인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
	INSUFFICIENT_FUNDS("보유 포인트가 부족합니다.", HttpStatus.BAD_REQUEST),
	INSUFFICIENT_QUANTITY("주문 수량이 부족합니다.", HttpStatus.BAD_REQUEST),
	SYSTEM_ERROR("시스템 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String message;
	private final HttpStatus status;

	Error(String message, HttpStatus status) {
		this.message = message;
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
