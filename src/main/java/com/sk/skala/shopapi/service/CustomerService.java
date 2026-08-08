package com.sk.skala.shopapi.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CancelRequest;
import com.sk.skala.shopapi.data.dto.CustomerDto;
import com.sk.skala.shopapi.data.dto.CustomerSession;
import com.sk.skala.shopapi.data.dto.OrderDto;
import com.sk.skala.shopapi.data.dto.OrderItemDto;
import com.sk.skala.shopapi.data.dto.OrderListDto;
import com.sk.skala.shopapi.data.dto.OrderRequest;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.OrderItem;
import com.sk.skala.shopapi.data.table.Orders;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.OrdersRepository;
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
	private final OrdersRepository ordersRepository;

	// 취소 상태값
	private static final String CANCELED = "CANCELED";

	// 회원가입 시 지급하는 초기 포인트
	@Value("${app.customer.initial-point:0}")
	private long initialPoint;

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

		// 기본값 세팅 - 초기 포인트 지급(요청값은 무시), 권한 USER
		customer.setCustomerPoint(initialPoint);
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
		List<OrderItemDto> products = orderItems.stream()
				.map(item -> OrderItemDto.builder()
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProductName())
						.unitPrice(item.getUnitPrice())
						.quantity(item.getQuantity())
						.subtotal(item.getSubtotal())
						.itemStatus(item.getItemStatus())
						.build())
				.toList();
		OrderListDto orderListDto = OrderListDto.builder()
				.customerId(customer.getCustomerId())
				.customerPoint(customer.getCustomerPoint() == null ? 0.0 : customer.getCustomerPoint().doubleValue())
				.products(products)
				.build();
		return Response.success(orderListDto);
	}

	// 상품주문 (포인트 차감)
	@Transactional
	public Response placeOrder(OrderRequest order) {

		// 1단계: 로그인한 고객 ID 얻기
		String customerId = sessionHandler.getCustomerId();

		// 2단계: 고객 조회 (없으면 DATA_NOT_FOUND)
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 3단계: 주문 상품이 비었는지 검증
		if (order.getItems() == null || order.getItems().isEmpty()) {
			throw new ParameterException("items");
		}

		// 4단계: 주문(Orders) 생성 + 배송지 정보 세팅
		Orders orders = new Orders();
		orders.setReceiverName(order.getReceiverName());
		orders.setReceiverPhone(order.getReceiverPhone());
		orders.setZipcode(order.getZipcode());
		orders.setAddress1(order.getAddress1());
		orders.setAddress2(order.getAddress2());
		orders.setDeliveryMemo(order.getDeliveryMemo());

		// 5단계: 상품 하나씩 처리
		long totalAmount = 0L;
		for (OrderRequest.OrderLine line : order.getItems()) {

			// 5-1. 상품 조회
			Product product = productRepository.findById(line.getProductId())
					.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

			// 5-2. 수량 검증
			if (line.getQuantity() == null || line.getQuantity() <= 0) {
				throw new ParameterException("quantity");
			}

			// 5-3. 재고 차감
			if (!product.decreaseStock(line.getQuantity())) {
				throw new ResponseException(Error.INSUFFICIENT_QUANTITY);
			}

			// 5-4. 주문 상세 생성
			OrderItem item = new OrderItem(product, line.getQuantity());

			// 5-5. 주문에 붙이기 (편의 메서드 사용)
			orders.addOrderItem(item);

			// 5-6. 총액 누적
			totalAmount += item.getSubtotal();
		}

		// 6단계: 포인트 차감
		if (!customer.usePoint(totalAmount)) {
			throw new ResponseException(Error.INSUFFICIENT_FUNDS);
		}

		// 7단계: 주문에 고객/총액 세팅 후 저장 (상세는 cascade 로 함께 저장됨)
		orders.setCustomer(customer);
		orders.setTotalAmount(totalAmount);
		Orders saved = ordersRepository.save(orders);

		// 8단계: 응답 조립
		List<OrderItemDto> items = saved.getOrderItems().stream()
				.map(item -> OrderItemDto.builder()
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProductName())
						.unitPrice(item.getUnitPrice())
						.quantity(item.getQuantity())
						.subtotal(item.getSubtotal())
						.itemStatus(item.getItemStatus())
						.build())
				.toList();

		OrderDto orderDto = OrderDto.builder()
				.orderId(saved.getId())
				.orderNumber(saved.getOrderNumber())
				.customerId(saved.getCustomer().getCustomerId())
				.totalAmount(saved.getTotalAmount())
				.status(saved.getStatus())
				.receiverName(saved.getReceiverName())
				.address1(saved.getAddress1())
				.address2(saved.getAddress2())
				.orderedAt(saved.getOrderedAt())
				.items(items)
				.build();

		return Response.success(orderDto);
	}

	// 주문 취소 (포인트 환급)
	@Transactional
	public Response cancelOrder(CancelRequest cancel) {

		String customerId = sessionHandler.getCustomerId();
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		if (cancel.getOrderId() == null) {
			throw new ParameterException("orderId");
		}

		Orders orders = ordersRepository.findById(cancel.getOrderId())
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 본인 주문인지 확인 - 남의 주문을 취소할 수 없음
		if (!orders.getCustomer().getCustomerId().equals(customerId)) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "본인의 주문만 취소할 수 있습니다.");
		}

		// 취소 대상 선별 - productId 가 null 이면 주문 전체
		List<OrderItem> targets;
		if (cancel.getProductId() == null) {
			targets = orders.getOrderItems();
		} else {
			targets = List.of(orderItemRepository
					.findByOrders_IdAndProduct_Id(orders.getId(), cancel.getProductId())
					.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND)));
		}

		// 환급액 계산 + 재고 복구
		long refund = 0L;
		int canceledCount = 0;
		for (OrderItem item : targets) {
			if (CANCELED.equals(item.getItemStatus())) {
				continue;
			}
			refund += item.getSubtotal();
			item.setItemStatus(CANCELED);
			canceledCount++;

			// 상품이 삭제된 경우는 재고 복구를 건너뜀
			if (item.getProduct() != null) {
				item.getProduct().increaseStock(item.getQuantity());
			}
		}

		if (canceledCount == 0) {
			throw new ResponseException(Error.DATA_NOT_FOUND, "이미 취소된 주문입니다.");
		}

		// 포인트 환급 + 주문 총액 갱신
		customer.restorePoint(refund);
		orders.setTotalAmount(orders.getTotalAmount() - refund);

		// 남은 항목이 없으면 주문 자체를 취소 상태로
		boolean allCanceled = orders.getOrderItems().stream()
				.allMatch(item -> CANCELED.equals(item.getItemStatus()));
		if (allCanceled) {
			orders.setStatus(CANCELED);
		}

		return Response.success(toOrderDto(orders));
	}

	// 고객 로그인 (JWT 발급 후 쿠키 저장)
	public Response loginCustomer(CustomerSession customerSession) {

		if (StringUtil.isAnyEmpty(customerSession.getCustomerId(), customerSession.getCustomerPassword())) {
			throw new ParameterException("customerId", "customerPassword");
		}

		// 아이디 존재 여부가 드러나지 않도록 실패 사유를 구분하지 않음
		Customer customer = customerRepository.findById(customerSession.getCustomerId())
				.orElseThrow(() -> new ResponseException(Error.NOT_AUTHENTICATED));

		if (!customer.getCustomerPassword().equals(customerSession.getCustomerPassword())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED);
		}

		// JWT 를 발급해 쿠키로 내려줌
		sessionHandler.storeAccessToken(customer.getCustomerId());

		return Response.success(toCustomerDto(customer));
	}

	// 고객 정보 수정
	@Transactional
	public Response updateCustomer(Customer customer) {

		String customerId = sessionHandler.getCustomerId();

		// 다른 사람의 ID 가 실려와도 세션 기준으로만 수정
		if (StringUtil.isNoneEmpty(customer.getCustomerId())
				&& !customerId.equals(customer.getCustomerId())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "본인 정보만 수정할 수 있습니다.");
		}

		Customer target = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 이메일은 바뀔 때만 중복 확인
		String email = customer.getEmail();
		if (StringUtil.isNoneEmpty(email) && !email.equals(target.getEmail())) {
			if (customerRepository.findByEmail(email).isPresent()) {
				throw new ResponseException(Error.DATA_DUPLICATED);
			}
			target.setEmail(email);
		}

		// 값이 들어온 항목만 반영 (포인트/권한/ID 는 수정 대상이 아님)
		if (StringUtil.isNoneEmpty(customer.getCustomerName())) {
			target.setCustomerName(customer.getCustomerName());
		}
		if (StringUtil.isNoneEmpty(customer.getPhone())) {
			target.setPhone(customer.getPhone());
		}
		if (StringUtil.isNoneEmpty(customer.getCustomerPassword())) {
			target.setCustomerPassword(customer.getCustomerPassword());
		}

		return Response.success(toCustomerDto(target));
	}

	// 고객 삭제 (회원 탈퇴)
	@Transactional
	public Response deleteCustomer(Customer customer) {

		String customerId = sessionHandler.getCustomerId();

		Customer target = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 비밀번호 재확인
		if (StringUtil.isAnyEmpty(customer.getCustomerPassword())
				|| !target.getCustomerPassword().equals(customer.getCustomerPassword())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "비밀번호가 일치하지 않습니다.");
		}

		// 주문 이력이 있으면 삭제 불가 - 주문 내역이 고아가 되는 것을 막음
		if (!ordersRepository.findByCustomer_CustomerIdOrderByOrderedAtDesc(customerId).isEmpty()) {
			throw new ResponseException(Error.DELETE_NOT_ALLOWED, "주문 이력이 있는 고객은 삭제할 수 없습니다.");
		}

		customerRepository.delete(target);
		sessionHandler.removeAccessToken();

		return Response.success();
	}

	// 주문 상세 목록을 응답 DTO 로 변환
	private List<OrderItemDto> toOrderItemDtos(List<OrderItem> orderItems) {
		return orderItems.stream()
				.map(item -> OrderItemDto.builder()
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProductName())
						.unitPrice(item.getUnitPrice())
						.quantity(item.getQuantity())
						.subtotal(item.getSubtotal())
						.itemStatus(item.getItemStatus())
						.build())
				.toList();
	}

	// 주문 1건을 응답 DTO 로 변환
	private OrderDto toOrderDto(Orders orders) {
		return OrderDto.builder()
				.orderId(orders.getId())
				.orderNumber(orders.getOrderNumber())
				.customerId(orders.getCustomer().getCustomerId())
				.totalAmount(orders.getTotalAmount())
				.status(orders.getStatus())
				.receiverName(orders.getReceiverName())
				.address1(orders.getAddress1())
				.address2(orders.getAddress2())
				.orderedAt(orders.getOrderedAt())
				.items(toOrderItemDtos(orders.getOrderItems()))
				.build();
	}

	// 고객 정보를 응답 DTO 로 변환 (비밀번호 제외)
	private CustomerDto toCustomerDto(Customer customer) {
		return CustomerDto.builder()
				.customerId(customer.getCustomerId())
				.customerName(customer.getCustomerName())
				.email(customer.getEmail())
				.phone(customer.getPhone())
				.customerPoint(customer.getCustomerPoint())
				.build();
	}
}
