package com.sk.skala.shopapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.Customer;

// 고객 데이터 저장소
public interface CustomerRepository extends JpaRepository<Customer, String> {

	// 이메일 중복 확인용
	Optional<Customer> findByEmail(String email);

	// 이름으로 조회 (동명이인 가능)
	List<Customer> findByCustomerName(String customerName);
}
