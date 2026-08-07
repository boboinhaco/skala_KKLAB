package com.sk.skala.shopapi.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.skala.shopapi.data.table.PointHistory;

// 포인트 이력 저장소
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

	// 특정 고객의 포인트 이력 (최신순)
	List<PointHistory> findByCustomer_CustomerIdOrderByCreatedAtDesc(String customerId);

	// 페이지 단위 조회
	Page<PointHistory> findByCustomer_CustomerId(String customerId, Pageable pageable);
}
