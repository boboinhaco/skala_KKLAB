package com.sk.skala.shopapi.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CustomerDto;
import com.sk.skala.shopapi.data.dto.CustomerSession;
import com.sk.skala.shopapi.data.dto.OrderListDto;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.PointHistory;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.OrdersRepository;
import com.sk.skala.shopapi.repository.PointHistoryRepository;
import com.sk.skala.shopapi.tools.StringUtil;

import lombok.RequiredArgsConstructor;

/**
 * 고객 관리 · 인증 비즈니스 로직.
 * 수정·삭제 대상은 요청 바디가 아니라 JWT 의 고객 ID 로 결정한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

	private static final String ROLE_USER = "USER";
	private static final String POINT_SIGNUP = "SIGNUP";

	private final CustomerRepository customerRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrdersRepository ordersRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final SessionHandler sessionHandler;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.customer.initial-point:0}")
	private long initialPoint;

	@Transactional(readOnly = true)
	public Response getAllCustomers(int offset, int count) {
		Page<Customer> page = customerRepository.findAll(PageRequest.of(offset, count));
		List<CustomerDto> customers = page.getContent().stream().map(CustomerDto::from).toList();

		return Response.success(new PagedList(page.getTotalElements(), offset, count, customers));
	}

	// 회원가입 - 초기 포인트를 지급한다.
	@Transactional
	public Response createCustomer(Customer customer) {
		if (StringUtil.isAnyEmpty(customer.getCustomerId(), customer.getCustomerPassword())) {
			throw new ParameterException("customerId", "customerPassword");
		}

		// save() 는 PK 가 이미 있으면 UPDATE 로 동작한다. 이 검사가 없으면 남의 계정 비밀번호를 덮어쓸 수 있다.
		if (customerRepository.existsById(customer.getCustomerId())) {
			throw new ResponseException(Error.DATA_DUPLICATED, "이미 사용 중인 아이디입니다.");
		}
		if (isEmailTaken(customer.getEmail())) {
			throw new ResponseException(Error.DATA_DUPLICATED, "이미 사용 중인 이메일입니다.");
		}

		customer.setCustomerPassword(passwordEncoder.encode(customer.getCustomerPassword()));

		// 포인트와 권한은 요청 값을 쓰지 않는다.
		customer.setCustomerPoint(initialPoint);
		customer.setRole(ROLE_USER);

		Customer saved = customerRepository.save(customer);
		pointHistoryRepository.save(new PointHistory(saved, initialPoint, POINT_SIGNUP, "회원가입 지급"));

		return Response.success(CustomerDto.from(saved));
	}

	// 고객 상세 + 주문한 상품 목록
	@Transactional(readOnly = true)
	public Response getCustomerById(String customerId) {
		Customer customer = findOrThrow(customerId);

		return Response.success(
				OrderListDto.of(customer, orderItemRepository.findByOrders_Customer_CustomerId(customerId)));
	}

	// 이름 조회 - 동명이인이 있을 수 있어 목록으로 반환
	@Transactional(readOnly = true)
	public Response getCustomersByName(String customerName) {
		List<Customer> customers = customerRepository.findByCustomerName(customerName);
		if (customers.isEmpty()) {
			throw new ResponseException(Error.DATA_NOT_FOUND);
		}

		return Response.success(customers.stream().map(CustomerDto::from).toList());
	}

	// 로그인 - 성공하면 JWT 를 쿠키로 내려준다.
	@Transactional(readOnly = true)
	public Response loginCustomer(CustomerSession session) {
		if (StringUtil.isAnyEmpty(session.getCustomerId(), session.getCustomerPassword())) {
			throw new ParameterException("customerId", "customerPassword");
		}

		// 아이디 존재 여부가 드러나지 않도록 실패 사유를 구분하지 않는다.
		Customer customer = customerRepository.findById(session.getCustomerId())
				.orElseThrow(() -> new ResponseException(Error.NOT_AUTHENTICATED));

		if (!passwordEncoder.matches(session.getCustomerPassword(), customer.getCustomerPassword())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED);
		}
		sessionHandler.storeAccessToken(customer.getCustomerId());

		return Response.success(CustomerDto.from(customer));
	}

	// 정보 수정 - 값이 들어온 항목만 반영한다. 포인트·권한·ID 는 수정 대상이 아니다.
	@Transactional
	public Response updateCustomer(Customer request) {
		Customer customer = findOwnAccount(request.getCustomerId(), "본인 정보만 수정할 수 있습니다.");

		String email = request.getEmail();
		if (StringUtil.isNoneEmpty(email) && !email.equals(customer.getEmail())) {
			if (isEmailTaken(email)) {
				throw new ResponseException(Error.DATA_DUPLICATED, "이미 사용 중인 이메일입니다.");
			}
			customer.setEmail(email);
		}
		setIfPresent(request.getCustomerName(), customer::setCustomerName);
		setIfPresent(request.getPhone(), customer::setPhone);
		if (StringUtil.isNoneEmpty(request.getCustomerPassword())) {
			customer.setCustomerPassword(passwordEncoder.encode(request.getCustomerPassword()));
		}

		return Response.success(CustomerDto.from(customer));
	}

	// 회원 탈퇴 - 되돌릴 수 없으므로 비밀번호를 다시 확인한다.
	@Transactional
	public Response deleteCustomer(Customer request) {
		Customer customer = findOwnAccount(request.getCustomerId(), "본인 계정만 삭제할 수 있습니다.");

		if (!passwordEncoder.matches(request.getCustomerPassword(), customer.getCustomerPassword())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "비밀번호가 일치하지 않습니다.");
		}
		// 주문 이력을 고아로 만들지 않기 위해 삭제를 막는다.
		if (!ordersRepository.findByCustomer_CustomerIdOrderByOrderedAtDesc(customer.getCustomerId()).isEmpty()) {
			throw new ResponseException(Error.DELETE_NOT_ALLOWED, "주문 이력이 있는 고객은 삭제할 수 없습니다.");
		}

		customerRepository.delete(customer);
		sessionHandler.removeAccessToken();

		return Response.success();
	}

	private Customer findOrThrow(String customerId) {
		return customerRepository.findById(customerId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));
	}

	// 요청에 다른 사람의 ID 가 실려와도 세션 기준으로만 처리한다.
	private Customer findOwnAccount(String requestedId, String message) {
		String customerId = sessionHandler.getCustomerId();
		if (StringUtil.isNoneEmpty(requestedId) && !customerId.equals(requestedId)) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, message);
		}
		return findOrThrow(customerId);
	}

	private boolean isEmailTaken(String email) {
		return StringUtil.isNoneEmpty(email) && customerRepository.findByEmail(email).isPresent();
	}

	private void setIfPresent(String value, Consumer<String> setter) {
		if (StringUtil.isNoneEmpty(value)) {
			setter.accept(value);
		}
	}
}
