package com.sk.skala.shopapi.data.table;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 포인트 증감 이력 - 잔액 변동의 근거를 남김
@Entity
@Table(name = "point_history")
@Getter
@Setter
@NoArgsConstructor
public class PointHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	private Long orderId;                                   // 주문 관련이 아니면 null

	@Column(nullable = false)
	private Long amount;                                    // 양수=적립, 음수=사용

	@Column(nullable = false)
	private Long balance;                                   // 처리 후 잔액

	@Column(nullable = false)
	private String type;                                    // SIGNUP, ORDER_USE, ORDER_REFUND 등

	private String reason;

	private LocalDateTime createdAt;

	public PointHistory(Customer customer, Long amount, String type, String reason) {
		this.customer = customer;
		this.amount = amount;
		this.balance = customer.getCustomerPoint();
		this.type = type;
		this.reason = reason;
	}

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
