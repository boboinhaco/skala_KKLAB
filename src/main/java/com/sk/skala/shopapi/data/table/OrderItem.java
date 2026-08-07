package com.sk.skala.shopapi.data.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 상세 - 주문 시점의 상품명과 가격을 스냅샷으로 보관
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Orders orders;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private String productName;                             // 주문 당시 상품명

	@Column(nullable = false)
	private Long unitPrice;                                 // 주문 당시 단가

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private String itemStatus = "ORDERED";

	// 주문 시점 정보를 복사해서 생성
	public OrderItem(Product product, Integer quantity) {
		this.product = product;
		this.productName = product.getProductName();
		this.unitPrice = product.getProductPrice();
		this.quantity = quantity;
	}

	// 이 항목의 소계
	public Long getSubtotal() {
		return this.unitPrice * this.quantity;
	}
}
