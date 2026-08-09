package com.sk.skala.shopapi.data.dto;

import com.sk.skala.shopapi.data.table.Product;

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
	private Long categoryId;
	private String productName;
	private Long productPrice;
	private String description;
	private String texture;
	private Integer soundLevel;
	private Integer stretchLevel;
	private String scent;
	private Integer stockQuantity;
	private String status;
	private Integer salesCount;
	private Integer reviewCount;
	private Integer likeCount;

	public static ProductDto from(Product product) {
		return ProductDto.builder()
				.id(product.getId())
				.categoryId(product.getCategoryId())
				.productName(product.getProductName())
				.productPrice(product.getProductPrice())
				.description(product.getDescription())
				.texture(product.getTexture())
				.soundLevel(product.getSoundLevel())
				.stretchLevel(product.getStretchLevel())
				.scent(product.getScent())
				.stockQuantity(product.getStockQuantity())
				.status(product.getStatus())
				.salesCount(product.getSalesCount())
				.reviewCount(product.getReviewCount())
				.likeCount(product.getLikeCount())
				.build();
	}
}
