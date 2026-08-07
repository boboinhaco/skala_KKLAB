package com.sk.skala.shopapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.Customer;

// 고객 데이터 저장소 - 사용자 정의 메서드 불필요
public interface CustomerRepository extends JpaRepository<Customer, String> {
}
