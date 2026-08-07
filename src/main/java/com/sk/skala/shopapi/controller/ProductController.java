package com.sk.skala.shopapi.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.service.ProductService;

import lombok.RequiredArgsConstructor;

// 상품 REST API
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	// 전체 상품 목록 조회
	@GetMapping
	public Response getAllProducts(@RequestParam(defaultValue = "0") Integer offset,
			@RequestParam(defaultValue = "10") Integer count) {
		return productService.getAllProducts(offset, count);
	}

	// 개별 상품 상세 조회
	@GetMapping("/{id}")
	public Response getProductById(@PathVariable Long id) {
		return productService.getProductById(id);
	}

	// 상품 등록
	@PostMapping
	public Response createProduct(@RequestBody Product product) {
		return productService.createProduct(product);
	}

	// 상품 정보 수정
	@PutMapping
	public Response updateProduct(@RequestBody Product product) {
		return productService.updateProduct(product);
	}

	// 상품 삭제
	@DeleteMapping
	public Response deleteProduct(@RequestBody Product product) {
		return productService.deleteProduct(product);
	}
}
