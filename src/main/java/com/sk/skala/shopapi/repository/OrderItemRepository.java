package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.OrderItem;
import com.sk.skala.shopapi.data.table.Product;

// 고객 주문 상품 매핑 저장소
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// 속성 순회로 특정 고객의 주문 목록 조회
	List<OrderItem> findByCustomer_CustomerId(String customerId);

	// 특정 고객이 특정 상품을 이미 주문했는지 확인
	Optional<OrderItem> findByCustomerAndProduct(Customer customer, Product product);
}
