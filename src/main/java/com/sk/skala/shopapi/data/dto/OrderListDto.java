package com.sk.skala.shopapi.data.dto;

import java.util.List;

import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.OrderItem;

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

	public static OrderListDto of(Customer customer, List<OrderItem> items) {
		Long point = customer.getCustomerPoint();

		return OrderListDto.builder()
				.customerId(customer.getCustomerId())
				.customerPoint(point == null ? 0.0 : point.doubleValue())
				.products(OrderItemDto.from(items))
				.build();
	}
}
