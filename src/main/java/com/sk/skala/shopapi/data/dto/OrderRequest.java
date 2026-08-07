package com.sk.skala.shopapi.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 및 주문 취소 요청 바디
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

	private Long productId;
	private Integer quantity;
}
