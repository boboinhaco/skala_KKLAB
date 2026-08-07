package com.sk.skala.shopapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.Product;

// 상품 데이터 저장소
public interface ProductRepository extends JpaRepository<Product, Long> {

	// 상품명 중복 확인용
	Optional<Product> findByProductName(String productName);
}
