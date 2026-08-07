package com.sk.skala.shopapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문한 개별 상품 응답
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

	private Long productId;
	private String productName;
	private Long unitPrice;
	private Integer quantity;
	private Long subtotal;
	private String itemStatus;
}
