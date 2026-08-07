package com.sk.skala.shopapi.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

// 모든 API가 공통으로 사용하는 응답 포맷
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

	public static final String SUCCESS = "success";
	public static final String FAIL = "fail";

	private String result;
	private String code;
	private String message;
	private Object body;

	public Response() {
		this.result = SUCCESS;
	}

	// 데이터 없는 성공 응답
	public static Response success() {
		return new Response();
	}

	// 데이터를 포함한 성공 응답
	public static Response success(Object body) {
		Response response = new Response();
		response.setBody(body);
		return response;
	}

	// 실패 응답
	public static Response error(String code, String message) {
		Response response = new Response();
		response.setResult(FAIL);
		response.setCode(code);
		response.setMessage(message);
		return response;
	}
}
