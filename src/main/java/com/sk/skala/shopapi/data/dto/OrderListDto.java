package com.sk.skala.shopapi.data.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 고객 주문 상품 목록 조회 응답
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListDto {

	private String customerId;
	private Double customerPoint;
	private List<OrderItemDto> products;
}
