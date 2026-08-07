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
import com.sk.skala.shopapi.data.table.OrderItem;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;
import com.sk.skala.shopapi.tools.StringUtil;

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

	// 고객 생성 (회원가입)
	public Response createCustomer(Customer customer) {
		// 필수 입력값 검증
		if (StringUtil.isAnyEmpty(customer.getCustomerId(), customer.getCustomerPassword())) {
			throw new ParameterException("customerId", "customerPassword");
		}

		// 아이디 중복 확인
		if (customerRepository.existsById(customer.getCustomerId())) {
			throw new ResponseException(Error.DATA_DUPLICATED);
		}

		// 이메일 중복 확인 (이메일은 선택 입력)
		if (StringUtil.isNoneEmpty(customer.getEmail())
				&& customerRepository.findByEmail(customer.getEmail()).isPresent()) {
			throw new ResponseException(Error.DATA_DUPLICATED);
		}

		// 기본값 세팅 - 포인트 0, 권한 USER
		if (customer.getCustomerPoint() == null) {
			customer.setCustomerPoint(0L);
		}
		if (StringUtil.isAnyEmpty(customer.getRole())) {
			customer.setRole("USER");
		}

		Customer saved = customerRepository.save(customer);

		// 응답에 비밀번호가 노출되지 않도록 제거(null)
		saved.setCustomerPassword(null);
		return Response.success(saved);
	}

	// 단일 고객 및 주문 상품 목록 조회
	@Transactional(readOnly = true)
	public Response getCustomerById(String customerId) {
		Customer customer = customerRepository.findById(customerId)
			.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));
		List<OrderItem> orderItems = orderItemRepository.findByOrders_Customer_CustomerId(customerId);

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
