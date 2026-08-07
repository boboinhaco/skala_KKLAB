package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 로그인 요청 바디 (ID / 비밀번호)
@Getter
@Setter
@NoArgsConstructor
public class CustomerSession {

	private String customerId;
	private String customerPassword;
}
