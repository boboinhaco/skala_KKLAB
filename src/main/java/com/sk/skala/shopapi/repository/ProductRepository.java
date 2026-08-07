package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.Product;

// 상품 데이터 저장소
public interface ProductRepository extends JpaRepository<Product, Long> {

	// 상품명 중복 확인용
	Optional<Product> findByProductName(String productName);

	// 카테고리별 목록 조회
	Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

	// 판매중인 상품만 조회
	Page<Product> findByStatus(String status, Pageable pageable);

	// 촉감별 필터 조회 (꾹꾹 리포트용)
	List<Product> findByTexture(String texture);
}
