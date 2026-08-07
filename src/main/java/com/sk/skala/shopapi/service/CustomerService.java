package com.sk.skala.shopapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CustomerSession;
import com.sk.skala.shopapi.data.dto.OrderRequest;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

// 고객 관리 및 주문 비즈니스 로직
@Service
@RequiredArgsConstructor
public class CustomerService {

	private final ProductRepository productRepository;
	private final CustomerRepository customerRepository;
	private final OrderItemRepository orderItemRepository;
	private final SessionHandler sessionHandler;

	// 전체 고객 목록 조회 (페이지 단위)
	public Response getAllCustomers(int offset, int count) {
		Pageable pageable = PageRequest.of(offset, count);
		Page<Customer> page = customerRepository.findAll(pageable);
		List<Customer> customers = page.getContent();

		// 응답에 비밀번호가 노출되지 않도록 제거(null)
    	for (Customer c : customers) {
        	c.setCustomerPassword(null);
    	}

		PagedList pagedList = new PagedList(page.getTotalElements(), offset, count, customers);
		return Response.success(pagedList);
	}

	// 단일 고객 및 주문 상품 목록 조회
	@Transactional(readOnly = true)
		public Response getCustomerById(String customerId) {
		throw new UnsupportedOperationException("TODO");
	}

	// 고객 생성 (회원가입)
	public Response createCustomer(Customer customer) {
		throw new UnsupportedOperationException("TODO");
	}

	// 상품주문 (포인트 차감)
	public Response placeOrder(OrderRequest order) {
		throw new UnsupportedOperationException("TODO");
	}

	// 주문 취소 (포인트 환급)
	public Response cancelOrder(OrderRequest order) {
		throw new UnsupportedOperationException("TODO");
	}

	public Response loginCustomer(CustomerSession customerSession) {
		throw new UnsupportedOperationException("TODO");
	}

	public Response updateCustomer(Customer customer) {
		throw new UnsupportedOperationException("TODO");
	}

	public Response deleteCustomer(Customer customer) {
		throw new UnsupportedOperationException("TODO");
	}
}
