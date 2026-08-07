package com.sk.skala.shopapi.data.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 1건 응답
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

	private Long orderId;
	private String orderNumber;
	private String customerId;
	private Long totalAmount;
	private String status;
	private String receiverName;
	private String address1;
	private String address2;
	private LocalDateTime orderedAt;
	private List<OrderItemDto> items;
}
