package com.sk.skala.shopapi.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CancelRequest;
import com.sk.skala.shopapi.data.dto.OrderDto;
import com.sk.skala.shopapi.data.dto.OrderRequest;
import com.sk.skala.shopapi.data.dto.OrderRequest.OrderLine;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.OrderItem;
import com.sk.skala.shopapi.data.table.PointHistory;
import com.sk.skala.shopapi.data.table.Orders;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.OrdersRepository;
import com.sk.skala.shopapi.repository.PointHistoryRepository;
import com.sk.skala.shopapi.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 · 취소 비즈니스 로직.
 * 주문자는 항상 JWT 에서 꺼내며, 요청 바디의 고객 정보는 신뢰하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

	static final String STATUS_CANCELED = "CANCELED";
	private static final String POINT_ORDER_USE = "ORDER_USE";
	private static final String POINT_ORDER_REFUND = "ORDER_REFUND";

	private final ProductRepository productRepository;
	private final CustomerRepository customerRepository;
	private final OrdersRepository ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final SessionHandler sessionHandler;

	// 상품 주문 - 재고 차감 + 포인트 차감을 한 트랜잭션으로 처리
	@Transactional
	public Response placeOrder(OrderRequest request) {
		List<OrderLine> lines = request.getItems();
		if (lines == null || lines.isEmpty()) {
			throw new ParameterException("items");
		}
		lines.forEach(this::validateLine);

		Customer customer = lockCustomer();
		Orders order = newOrder(request, customer);

		long totalAmount = 0L;
		// 상품 ID 오름차순으로 잠가야 서로 다른 순서로 잠그다 교착에 빠지지 않는다.
		for (OrderLine line : sortedByProductId(lines)) {
			Product product = productRepository.findByIdForUpdate(line.getProductId())
					.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

			if (!product.decreaseStock(line.getQuantity())) {
				throw new ResponseException(Error.INSUFFICIENT_QUANTITY, product.getProductName() + " 재고가 부족합니다.");
			}

			OrderItem item = new OrderItem(product, line.getQuantity());
			order.addOrderItem(item);
			totalAmount += item.getSubtotal();
		}

		// 상품별로 나눠 깎지 않고 총액을 한 번에 차감해야 부분 결제된 주문이 생기지 않는다.
		if (!customer.usePoint(totalAmount)) {
			throw new ResponseException(Error.INSUFFICIENT_FUNDS);
		}
		order.setTotalAmount(totalAmount);

		// 주문 상세는 cascade, 재고·포인트는 더티 체킹으로 함께 반영된다.
		Orders saved = ordersRepository.save(order);
		writeHistory(customer, saved.getId(), -totalAmount, POINT_ORDER_USE, saved.getOrderNumber() + " 주문");

		return Response.success(OrderDto.from(saved));
	}

	// 로그인한 고객의 주문 목록 (최신순)
	@Transactional(readOnly = true)
	public Response getMyOrders() {
		List<OrderDto> orders = ordersRepository.findWithItemsByCustomerId(sessionHandler.getCustomerId()).stream()
				.sorted(Comparator.comparing(Orders::getOrderedAt).reversed())
				.map(OrderDto::from)
				.toList();

		return Response.success(orders);
	}

	// 주문 취소 - productId 를 주면 그 상품만, 없으면 주문 전체
	@Transactional
	public Response cancelOrder(CancelRequest request) {
		if (request.getOrderId() == null) {
			throw new ParameterException("orderId");
		}

		Customer customer = lockCustomer();
		Orders order = ordersRepository.findById(request.getOrderId())
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "본인의 주문만 취소할 수 있습니다.");
		}

		long refund = cancelItems(targetsOf(order, request.getProductId()));
		if (refund == 0L) {
			throw new ResponseException(Error.DATA_NOT_FOUND, "이미 취소된 주문입니다.");
		}

		customer.restorePoint(refund);
		order.setTotalAmount(order.getTotalAmount() - refund);
		writeHistory(customer, order.getId(), refund, POINT_ORDER_REFUND, order.getOrderNumber() + " 취소");

		if (order.getOrderItems().stream().allMatch(OrderService::isCanceled)) {
			order.setStatus(STATUS_CANCELED);
		}

		return Response.success(OrderDto.from(order));
	}

	// 포인트를 바꾸는 흐름이므로 행을 잠근 채 읽는다.
	private Customer lockCustomer() {
		return customerRepository.findByIdForUpdate(sessionHandler.getCustomerId())
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));
	}

	private List<OrderLine> sortedByProductId(List<OrderLine> lines) {
		return lines.stream()
				.sorted(Comparator.comparing(OrderLine::getProductId))
				.toList();
	}

	// 배송지는 주문 시점 값을 그대로 보관한다. (고객이 주소를 바꿔도 과거 주문은 유지)
	private Orders newOrder(OrderRequest request, Customer customer) {
		Orders order = new Orders();
		order.setCustomer(customer);
		order.setReceiverName(request.getReceiverName());
		order.setReceiverPhone(request.getReceiverPhone());
		order.setZipcode(request.getZipcode());
		order.setAddress1(request.getAddress1());
		order.setAddress2(request.getAddress2());
		order.setDeliveryMemo(request.getDeliveryMemo());
		return order;
	}

	// 음수 수량을 허용하면 재고와 포인트가 오히려 늘어난다.
	private void validateLine(OrderLine line) {
		if (line.getProductId() == null) {
			throw new ParameterException("productId");
		}
		if (line.getQuantity() == null || line.getQuantity() <= 0) {
			throw new ParameterException("quantity");
		}
	}

	private List<OrderItem> targetsOf(Orders order, Long productId) {
		if (productId == null) {
			return order.getOrderItems();
		}
		return List.of(orderItemRepository.findByOrders_IdAndProduct_Id(order.getId(), productId)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND)));
	}

	// 이미 취소된 항목은 건너뛴다. 같은 요청을 두 번 보내도 중복 환급되지 않도록.
	private long cancelItems(List<OrderItem> targets) {
		long refund = 0L;

		for (OrderItem item : targets) {
			if (isCanceled(item)) {
				continue;
			}
			item.setItemStatus(STATUS_CANCELED);
			refund += item.getSubtotal();

			// 삭제된 상품은 복구할 대상이 없으므로 건너뛴다.
			if (item.getProduct() != null) {
				productRepository.findByIdForUpdate(item.getProduct().getId())
						.ifPresent(product -> product.increaseStock(item.getQuantity()));
			}
		}
		return refund;
	}

	// 잔액 변동의 근거를 남긴다. amount 는 사용이면 음수, 환급이면 양수.
	private void writeHistory(Customer customer, Long orderId, long amount, String type, String reason) {
		PointHistory history = new PointHistory(customer, amount, type, reason);
		history.setOrderId(orderId);
		pointHistoryRepository.save(history);
	}

	private static boolean isCanceled(OrderItem item) {
		return STATUS_CANCELED.equals(item.getItemStatus());
	}
}
