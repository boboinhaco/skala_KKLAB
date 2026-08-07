package com.sk.skala.shopapi.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.dto.CustomerSession;
import com.sk.skala.shopapi.data.dto.OrderRequest;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.service.CustomerService;

import lombok.RequiredArgsConstructor;

// 고객 및 주문 REST API
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	// 전체 고객 목록 조회
	@GetMapping
	public Response getAllCustomers(@RequestParam(value = "offset", defaultValue = "0") int offset,
			@RequestParam(value = "count", defaultValue = "10") int count) {
		return customerService.getAllCustomers(offset, count);
	}

	// 단일 고객 상세 및 주문 목록 조회
	@GetMapping("/{customerId}")
	public Response getCustomerById(@PathVariable String customerId) {
		return customerService.getCustomerById(customerId);
	}

	// 고객 등록 (회원가입)
	@PostMapping
	public Response createCustomer(@RequestBody Customer customer) {
		return customerService.createCustomer(customer);
	}

	// 고객 로그인
	@PostMapping("/login")
	public Response loginCustomer(@RequestBody CustomerSession customerSession) {
		return customerService.loginCustomer(customerSession);
	}

	// 고객 정보 수정
	@PutMapping
	public Response updateCustomer(@RequestBody Customer customer) {
		return customerService.updateCustomer(customer);
	}

	// 고객 삭제
	@DeleteMapping
	public Response deleteCustomer(@RequestBody Customer customer) {
		return customerService.deleteCustomer(customer);
	}

	// 상품 주문
	@PostMapping("/order")
	public Response placeOrder(@RequestBody OrderRequest order) {
		return customerService.placeOrder(order);
	}

	// 주문 취소
	@PostMapping("/cancel")
	public Response cancelOrder(@RequestBody OrderRequest order) {
		return customerService.cancelOrder(order);
	}
}
