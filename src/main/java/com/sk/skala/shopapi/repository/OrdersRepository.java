package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.skala.shopapi.data.table.Orders;

// 주문 데이터 저장소
public interface OrdersRepository extends JpaRepository<Orders, Long> {

	// 특정 고객의 주문 목록 (최신순)
	List<Orders> findByCustomer_CustomerIdOrderByOrderedAtDesc(String customerId);

	// 특정 고객의 주문 목록 (페이지 단위)
	Page<Orders> findByCustomer_CustomerId(String customerId, Pageable pageable);

	// 주문번호로 조회
	Optional<Orders> findByOrderNumber(String orderNumber);

	// 주문과 상세를 한 번에 조회해 N+1 방지
	@Query("select distinct o from Orders o join fetch o.orderItems where o.customer.customerId = :customerId")
	List<Orders> findWithItemsByCustomerId(String customerId);
}
