package com.sk.skala.shopapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CancelRequest;
import com.sk.skala.shopapi.data.dto.OrderDto;
import com.sk.skala.shopapi.data.dto.OrderRequest;
import com.sk.skala.shopapi.data.dto.OrderRequest.OrderLine;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.PointHistory;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.PointHistoryRepository;
import com.sk.skala.shopapi.repository.ProductRepository;

/**
 * 주문·취소는 포인트와 재고를 동시에 건드리므로 트랜잭션 경계를 실제로 확인한다.
 * 롤백과 동시성을 보려면 커밋이 일어나야 해서 테스트에 @Transactional 을 붙이지 않는다.
 */
@SpringBootTest
class OrderServiceTest {

	@Autowired
	private OrderService orderService;
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private PointHistoryRepository pointHistoryRepository;

	@MockBean
	private SessionHandler sessionHandler;

	private static final AtomicInteger SEQ = new AtomicInteger();

	private Customer givenCustomer(long point) {
		Customer customer = new Customer("buyer" + SEQ.incrementAndGet(), "pw", point);
		customerRepository.save(customer);
		given(sessionHandler.getCustomerId()).willReturn(customer.getCustomerId());
		return customer;
	}

	private Product givenProduct(long price, int stock) {
		return productRepository.save(new Product("상품" + SEQ.incrementAndGet(), price, stock));
	}

	private OrderRequest orderOf(Product product, int quantity) {
		OrderLine line = new OrderLine();
		line.setProductId(product.getId());
		line.setQuantity(quantity);

		OrderRequest request = new OrderRequest();
		request.setItems(List.of(line));
		request.setReceiverName("받는이");
		request.setAddress1("서울");
		return request;
	}

	private long pointOf(Customer customer) {
		return customerRepository.findById(customer.getCustomerId()).orElseThrow().getCustomerPoint();
	}

	private int stockOf(Product product) {
		return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
	}

	@Test
	@DisplayName("주문하면 포인트가 차감되고 재고가 줄어든다")
	void placeOrder_deductsPointAndStock() {
		Customer customer = givenCustomer(10_000L);
		Product product = givenProduct(3_000L, 10);

		OrderDto order = (OrderDto) orderService.placeOrder(orderOf(product, 2)).getBody();

		assertThat(order.getTotalAmount()).isEqualTo(6_000L);
		assertThat(order.getItems()).hasSize(1);
		assertThat(pointOf(customer)).isEqualTo(4_000L);
		assertThat(stockOf(product)).isEqualTo(8);
	}

	@Test
	@DisplayName("포인트가 부족하면 주문이 거부되고 재고도 원래대로 돌아간다")
	void placeOrder_rollsBackWhenPointIsNotEnough() {
		Customer customer = givenCustomer(1_000L);
		Product product = givenProduct(3_000L, 10);

		assertThatThrownBy(() -> orderService.placeOrder(orderOf(product, 1)))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.INSUFFICIENT_FUNDS);

		// 재고는 포인트 검증보다 먼저 차감되므로 롤백되지 않으면 9가 된다.
		assertThat(stockOf(product)).isEqualTo(10);
		assertThat(pointOf(customer)).isEqualTo(1_000L);
	}

	@Test
	@DisplayName("재고보다 많이 주문하면 거부된다")
	void placeOrder_rejectsWhenStockIsNotEnough() {
		givenCustomer(100_000L);
		Product product = givenProduct(1_000L, 2);

		assertThatThrownBy(() -> orderService.placeOrder(orderOf(product, 3)))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.INSUFFICIENT_QUANTITY);
	}

	@Test
	@DisplayName("수량이 0 이하이면 상품을 조회하기 전에 거부된다")
	void placeOrder_rejectsNonPositiveQuantity() {
		givenCustomer(100_000L);
		Product product = givenProduct(1_000L, 10);

		assertThatThrownBy(() -> orderService.placeOrder(orderOf(product, 0)))
				.isInstanceOf(ParameterException.class);
		assertThatThrownBy(() -> orderService.placeOrder(orderOf(product, -5)))
				.isInstanceOf(ParameterException.class);

		assertThat(stockOf(product)).isEqualTo(10);
	}

	@Test
	@DisplayName("주문을 취소하면 포인트가 환급되고 재고가 복구된다")
	void cancelOrder_refundsPointAndRestoresStock() {
		Customer customer = givenCustomer(10_000L);
		Product product = givenProduct(3_000L, 10);
		OrderDto order = (OrderDto) orderService.placeOrder(orderOf(product, 2)).getBody();

		CancelRequest cancel = new CancelRequest();
		cancel.setOrderId(order.getOrderId());
		OrderDto canceled = (OrderDto) orderService.cancelOrder(cancel).getBody();

		assertThat(canceled.getStatus()).isEqualTo("CANCELED");
		assertThat(canceled.getTotalAmount()).isZero();
		assertThat(pointOf(customer)).isEqualTo(10_000L);
		assertThat(stockOf(product)).isEqualTo(10);
	}

	@Test
	@DisplayName("이미 취소한 주문을 다시 취소해도 포인트가 중복 환급되지 않는다")
	void cancelOrder_doesNotRefundTwice() {
		Customer customer = givenCustomer(10_000L);
		Product product = givenProduct(3_000L, 10);
		OrderDto order = (OrderDto) orderService.placeOrder(orderOf(product, 1)).getBody();

		CancelRequest cancel = new CancelRequest();
		cancel.setOrderId(order.getOrderId());
		orderService.cancelOrder(cancel);

		assertThatThrownBy(() -> orderService.cancelOrder(cancel))
				.isInstanceOf(ResponseException.class)
				.hasMessageContaining("이미 취소된 주문");

		assertThat(pointOf(customer)).isEqualTo(10_000L);
		assertThat(stockOf(product)).isEqualTo(10);
	}

	@Test
	@DisplayName("주문과 취소가 포인트 이력에 사용·환급으로 남는다")
	void pointHistory_recordsUseAndRefund() {
		Customer customer = givenCustomer(10_000L);
		Product product = givenProduct(3_000L, 10);
		OrderDto order = (OrderDto) orderService.placeOrder(orderOf(product, 2)).getBody();

		CancelRequest cancel = new CancelRequest();
		cancel.setOrderId(order.getOrderId());
		orderService.cancelOrder(cancel);

		List<PointHistory> histories =
				pointHistoryRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customer.getCustomerId());

		// 이 테스트는 고객을 저장소로 직접 만들었으므로 가입(SIGNUP) 이력은 없다.
		assertThat(histories).extracting(PointHistory::getType)
				.containsExactlyInAnyOrder("ORDER_USE", "ORDER_REFUND");

		PointHistory use = histories.stream().filter(h -> "ORDER_USE".equals(h.getType())).findFirst().orElseThrow();
		PointHistory refund = histories.stream().filter(h -> "ORDER_REFUND".equals(h.getType())).findFirst()
				.orElseThrow();

		assertThat(use.getAmount()).isEqualTo(-6_000L);      // 사용은 음수
		assertThat(use.getBalance()).isEqualTo(4_000L);      // 처리 후 잔액
		assertThat(refund.getAmount()).isEqualTo(6_000L);    // 환급은 양수
		assertThat(refund.getBalance()).isEqualTo(10_000L);
		assertThat(use.getOrderId()).isEqualTo(order.getOrderId());
	}

	@Test
	@DisplayName("다른 사람의 주문은 취소할 수 없다")
	void cancelOrder_rejectsOtherCustomersOrder() {
		givenCustomer(10_000L);
		Product product = givenProduct(1_000L, 10);
		OrderDto order = (OrderDto) orderService.placeOrder(orderOf(product, 1)).getBody();

		// 다른 고객으로 로그인한 상태를 만든다.
		givenCustomer(10_000L);

		CancelRequest cancel = new CancelRequest();
		cancel.setOrderId(order.getOrderId());

		assertThatThrownBy(() -> orderService.cancelOrder(cancel))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.NOT_AUTHENTICATED);
	}

	@Test
	@DisplayName("같은 고객이 동시에 주문해도 포인트가 정확히 차감된다")
	void placeOrder_isSafeUnderConcurrency() throws Exception {
		Customer customer = givenCustomer(10_000L);
		Product product = givenProduct(1_000L, 100);

		int threads = 8;
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			new Thread(() -> {
				ready.countDown();
				try {
					start.await();
					orderService.placeOrder(orderOf(product, 1));
				} catch (Exception ignored) {
					// 실패 건은 아래 잔액 검증에서 자연히 드러난다.
				} finally {
					done.countDown();
				}
			}).start();
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(20, TimeUnit.SECONDS);

		// 잠금이 없으면 차감이 유실되어 잔액이 10,000 에 가깝게 남는다.
		assertThat(pointOf(customer)).isEqualTo(10_000L - (threads * 1_000L));
		assertThat(stockOf(product)).isEqualTo(100 - threads);
	}
}
