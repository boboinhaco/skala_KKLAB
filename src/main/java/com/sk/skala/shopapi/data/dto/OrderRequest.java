package com.sk.skala.shopapi.data.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 요청 바디 - 여러 상품을 한 번에 주문
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

	private List<OrderLine> items;
	private String receiverName;
	private String receiverPhone;
	private String zipcode;
	private String address1;
	private String address2;
	private String deliveryMemo;

	// 주문할 상품 한 줄
	@Getter
	@Setter
	@NoArgsConstructor
	public static class OrderLine {
		private Long productId;
		private Integer quantity;
	}
}
