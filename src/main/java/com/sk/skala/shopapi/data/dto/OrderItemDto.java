package com.sk.skala.shopapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 고객이 주문한 개별 상품 정보 (상품 + 수량)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

	private Long productId;
	private String productName;
	private Double productPrice;
	private Integer quantity;
}
