package com.sk.skala.shopapi.data.table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 주문 헤더 - ORDER는 SQL 예약어라 테이블명은 orders
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Orders {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Column(nullable = false)
	private Long totalAmount;

	@Column(nullable = false)
	private String status = "PAID";

	private String receiverName;                            // 주문 시점 배송지 스냅샷
	private String receiverPhone;
	private String zipcode;
	private String address1;
	private String address2;
	private String deliveryMemo;

	private LocalDateTime orderedAt;

	// 주문과 생명주기를 함께하므로 cascade + orphanRemoval 적용
	@OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> orderItems = new ArrayList<>();

	public Orders(Customer customer, Long totalAmount) {
		this.customer = customer;
		this.totalAmount = totalAmount;
	}

	@PrePersist
	public void prePersist() {
		this.orderedAt = LocalDateTime.now();
		if (this.orderNumber == null) {
			this.orderNumber = generateOrderNumber();
		}
	}

	// 주문번호 생성 - KK + 날짜시각 형식
	private String generateOrderNumber() {
		return "KK" + LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 14);
	}

	// 양방향 연관관계 편의 메서드
	public void addOrderItem(OrderItem item) {
		this.orderItems.add(item);
		item.setOrders(this);
	}
}
