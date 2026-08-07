package com.sk.skala.shopapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 상품 응답 - 꾹꾹 리포트 항목 포함
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

	private Long id;
	private String productName;
	private Long productPrice;
	private String description;
	private String texture;
	private Integer soundLevel;
	private Integer stretchLevel;
	private String scent;
	private Integer stockQuantity;
	private String status;
}
