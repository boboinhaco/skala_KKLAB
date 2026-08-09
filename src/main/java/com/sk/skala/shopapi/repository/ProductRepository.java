package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.skala.shopapi.data.table.Product;

import jakarta.persistence.LockModeType;

// 상품 데이터 저장소
public interface ProductRepository extends JpaRepository<Product, Long> {

	// 재고 차감·복구용 조회 (동시 주문 시 재고가 덜 빠지는 것을 막는다)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);

	// 상품명 중복 확인용
	Optional<Product> findByProductName(String productName);

	// 카테고리별 목록 조회
	Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

	// 판매중인 상품만 조회
	Page<Product> findByStatus(String status, Pageable pageable);

	// 촉감별 필터 조회 (꾹꾹 리포트용)
	List<Product> findByTexture(String texture);
}
