package com.sk.skala.shopapi.data.dto;

import com.sk.skala.shopapi.data.table.Customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 고객 정보 응답 - 비밀번호를 아예 담지 않음
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

	private String customerId;
	private String customerName;
	private String email;
	private String phone;
	private Long customerPoint;

	public static CustomerDto from(Customer customer) {
		return CustomerDto.builder()
				.customerId(customer.getCustomerId())
				.customerName(customer.getCustomerName())
				.email(customer.getEmail())
				.phone(customer.getPhone())
				.customerPoint(customer.getCustomerPoint())
				.build();
	}
}
