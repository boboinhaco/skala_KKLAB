package com.sk.skala.shopapi.data.table;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

	private String customerPassword;

	private Double customerPoint;

	public Customer(String customerId, Double customerPoint) {
		this.customerId = customerId;
		this.customerPoint = customerPoint;
	}
}
