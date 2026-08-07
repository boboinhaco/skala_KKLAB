package com.sk.skala.shopapi.data.table;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 상품 엔터티 - ID 자동 증가
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String productName;

	private Double productPrice;

	public Product(String productName, Double productPrice) {
		this.productName = productName;
		this.productPrice = productPrice;
	}
}
