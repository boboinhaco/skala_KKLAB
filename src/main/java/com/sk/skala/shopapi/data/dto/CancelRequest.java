package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 취소 요청 바디 - 부분 취소 지원
@Getter
@Setter
@NoArgsConstructor
public class CancelRequest {

	private Long orderId;
	private Long productId;                                 // null이면 주문 전체 취소
	private Integer quantity;
	private String reasonCode;
	private String reasonDetail;
}
