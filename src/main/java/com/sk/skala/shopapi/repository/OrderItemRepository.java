package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.OrderItem;

// 주문 상세 저장소
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// 주문 1건의 상세 목록
	List<OrderItem> findByOrders_Id(Long orderId);

	// 특정 주문에서 특정 상품을 찾기 (취소 처리용)
	Optional<OrderItem> findByOrders_IdAndProduct_Id(Long orderId, Long productId);

	// 특정 고객이 주문한 전체 상세 목록
	List<OrderItem> findByOrders_Customer_CustomerId(String customerId);

	// 해당 상품의 주문 이력 존재 여부 (상품 삭제 가능 여부 판단용)
	boolean existsByProduct_Id(Long productId);
}
