package com.sk.skala.shopapi.data.table;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 고객 엔터티 - PK는 문자열 customerId
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

	@Id
	private String customerId;

	@Column(nullable = false)
	private String customerPassword;

	private String customerName;

	@Column(unique = true)
	private String email;

	private String phone;

	@Column(nullable = false)
	private Long customerPoint = 0L;

	@Column(nullable = false)
	private String role = "USER";

	private LocalDateTime createdAt;

	public Customer(String customerId, String customerPassword, Long customerPoint) {
		this.customerId = customerId;
		this.customerPassword = customerPassword;
		this.customerPoint = customerPoint;
	}

	// 저장 직전 생성 시각 자동 세팅
	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	// 포인트 차감 - 잔액 부족 시 false 반환
	public boolean usePoint(long amount) {
		if (this.customerPoint < amount) {
			return false;
		}
		this.customerPoint -= amount;
		return true;
	}

	// 포인트 환급
	public void restorePoint(long amount) {
		this.customerPoint += amount;
	}
}
