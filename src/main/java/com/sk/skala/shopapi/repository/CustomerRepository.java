package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.skala.shopapi.data.table.Customer;

import jakarta.persistence.LockModeType;

// 고객 데이터 저장소
public interface CustomerRepository extends JpaRepository<Customer, String> {

	/**
	 * 포인트를 변경할 때 쓰는 조회.
	 * 일반 findById 로 읽고 차감하면 동시 주문 시 나중 커밋이 앞선 차감을 덮어써(lost update)
	 * 포인트가 덜 빠진다. 행 잠금으로 한 번에 하나씩만 처리되게 한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Customer c where c.customerId = :customerId")
	Optional<Customer> findByIdForUpdate(@Param("customerId") String customerId);

	// 이메일 중복 확인용
	Optional<Customer> findByEmail(String email);

	// 이름으로 조회 (동명이인 가능)
	List<Customer> findByCustomerName(String customerName);
}
