package com.sk.skala.shopapi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
		List<Customer> customers = page.getContent(); // 비밀번호 null로 세팅

		// 응답에 비밀번호가 노출되지 않도록 제거
    	for (Customer c : customers) {
        	c.setCustomerPassword(null);
    	}

		PagedList pagedList = new PagedList(page.getTotalElements(), offset, count, customers);
		return Response.success(pagedList);
	}

	// 단일 고객 및 주문 상품 목록 조회
	@Transactional(readOnly = true)
	public Response getCustomerById(String customerId) {
		// TODO 1. customerRepository.findById로 조회, 없으면 Error.DATA_NOT_FOUND
		// TODO 2. orderItemRepository.findByCustomer_CustomerId로 주문 목록 조회
		// TODO 3. Stream API로 OrderItemDto 리스트 변환
		// TODO 4. OrderListDto 빌더로 조립 후 Response.success 반환
		throw new UnsupportedOperationException("TODO: getCustomerById 구현");
	}

	// 고객 생성 (회원가입)
	public Response createCustomer(Customer customer) {
		// TODO 1. StringUtil.isAnyEmpty로 ID/PW 검증, 실패 시 ParameterException
		// TODO 2. 중복 ID 확인, 있으면 Error.DATA_DUPLICATED
		// TODO 3. 초기 포인트 세팅 후 save
		// TODO 4. 비밀번호는 null 처리하고 Response.success 반환
		throw new UnsupportedOperationException("TODO: createCustomer 구현");
	}

	// 고객 로그인
	public Response loginCustomer(CustomerSession customerSession) {
		// TODO 1. ID/PW 입력값 검증, 실패 시 ParameterException
		// TODO 2. ID로 조회, 없으면 Error.DATA_NOT_FOUND
		// TODO 3. 비밀번호 불일치 시 Error.NOT_AUTHENTICATED
		// TODO 4. sessionHandler.storeAccessToken(customerId) 호출
		// TODO 5. 비밀번호 null 처리 후 Response.success 반환
		throw new UnsupportedOperationException("TODO: loginCustomer 구현");
	}

	// 고객 정보 수정
	public Response updateCustomer(Customer customer) {
		// TODO 1. customerId, customerPoint 유효성 확인
		// TODO 2. 존재 확인, 없으면 Error.DATA_NOT_FOUND
		// TODO 3. 포인트 갱신 후 save, Response.success 반환
		throw new UnsupportedOperationException("TODO: updateCustomer 구현");
	}

	// 고객 삭제
	public Response deleteCustomer(Customer customer) {
		// TODO 1. customerId로 존재 확인, 없으면 Error.DATA_NOT_FOUND
		// TODO 2. delete 후 Response.success 반환
		throw new UnsupportedOperationException("TODO: deleteCustomer 구현");
	}

	// 상품 주문 (포인트 차감)
	@Transactional
	public Response placeOrder(OrderRequest order) {
		// TODO 1. sessionHandler.getCustomerId()로 로그인 고객 확인
		// TODO 2. Customer, Product 조회, 없으면 Error.DATA_NOT_FOUND
		// TODO 3. 총 금액 = 가격 * 수량, 포인트 부족 시 Error.INSUFFICIENT_FUNDS
		// TODO 4. 포인트 차감
		// TODO 5. findByCustomerAndProduct로 기존 주문 있으면 수량 누적, 없으면 신규 생성
		// TODO 6. 남은 포인트를 담아 Response.success 반환
		throw new UnsupportedOperationException("TODO: placeOrder 구현");
	}

	// 주문 취소 (포인트 환급)
	@Transactional
	public Response cancelOrder(OrderRequest order) {
		// TODO 1. sessionHandler.getCustomerId()로 로그인 고객 확인
		// TODO 2. Customer, Product 조회, 없으면 Error.DATA_NOT_FOUND
		// TODO 3. OrderItem 조회 후 보유 수량 부족 시 Error.INSUFFICIENT_QUANTITY
		// TODO 4. 수량 차감, 0이면 OrderItem 삭제
		// TODO 5. 취소 금액만큼 포인트 환급
		// TODO 6. 남은 포인트를 담아 Response.success 반환
		throw new UnsupportedOperationException("TODO: cancelOrder 구현");
	}
}
