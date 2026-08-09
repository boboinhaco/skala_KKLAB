package com.sk.skala.shopapi.data.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sk.skala.shopapi.data.table.Orders;

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

	public static OrderDto from(Orders orders) {
		return OrderDto.builder()
				.orderId(orders.getId())
				.orderNumber(orders.getOrderNumber())
				.customerId(orders.getCustomer().getCustomerId())
				.totalAmount(orders.getTotalAmount())
				.status(orders.getStatus())
				.receiverName(orders.getReceiverName())
				.address1(orders.getAddress1())
				.address2(orders.getAddress2())
				.orderedAt(orders.getOrderedAt())
				.items(OrderItemDto.from(orders.getOrderItems()))
				.build();
	}
}
