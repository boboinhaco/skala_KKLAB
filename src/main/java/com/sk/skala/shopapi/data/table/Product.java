package com.sk.skala.shopapi.data.table;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 상품 엔터티 - 옵션 없이 상품 단위로 재고 관리
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long categoryId;

	@Column(nullable = false)
	private String productName;

	@Column(nullable = false)
	private Long productPrice;

	@Column(length = 2000)
	private String description;

	private String texture;                                 // 촉감: 말랑, 크런치, 버터 등

	private Integer soundLevel;                             // ASMR 소리 강도 1~5

	private Integer stretchLevel;                           // 늘어남 정도 1~5

	private String scent;

	@Column(nullable = false)
	private Integer stockQuantity = 0;

	@Column(nullable = false)
	private String status = "ON_SALE";

	private Integer salesCount = 0;                         // 누적 판매 수량 (판매순 정렬용)

	private Integer reviewCount = 0;                        // 후기 수 (후기순 정렬용)

	private Integer likeCount = 0;                          // 찜 수 (인기순 정렬용)

	private LocalDateTime createdAt;

	public Product(String productName, Long productPrice, Integer stockQuantity) {
		this.productName = productName;
		this.productPrice = productPrice;
		this.stockQuantity = stockQuantity;
	}

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	// 재고 차감 - 부족 시 false 반환
	public boolean decreaseStock(int quantity) {
		if (this.stockQuantity < quantity) {
			return false;
		}
		this.stockQuantity -= quantity;
		return true;
	}

	// 재고 복구 (주문 취소 시)
	public void increaseStock(int quantity) {
		this.stockQuantity += quantity;
	}
}
