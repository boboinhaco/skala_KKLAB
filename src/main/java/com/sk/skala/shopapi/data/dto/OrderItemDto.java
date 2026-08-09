package com.sk.skala.shopapi.data.dto;

import java.util.List;

import com.sk.skala.shopapi.data.table.OrderItem;

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

	public static OrderItemDto from(OrderItem item) {
		// 상품이 삭제된 과거 주문이면 productId 만 비고, 상품명·단가는 주문 시점 값을 그대로 쓴다.
		return OrderItemDto.builder()
				.productId(item.getProduct() == null ? null : item.getProduct().getId())
				.productName(item.getProductName())
				.unitPrice(item.getUnitPrice())
				.quantity(item.getQuantity())
				.subtotal(item.getSubtotal())
				.itemStatus(item.getItemStatus())
				.build();
	}

	public static List<OrderItemDto> from(List<OrderItem> items) {
		return items.stream().map(OrderItemDto::from).toList();
	}
}
