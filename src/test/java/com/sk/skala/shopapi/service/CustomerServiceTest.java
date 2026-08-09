package com.sk.skala.shopapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.SessionHandler;
import com.sk.skala.shopapi.data.dto.CustomerDto;
import com.sk.skala.shopapi.data.dto.CustomerSession;
import com.sk.skala.shopapi.data.table.Customer;
import com.sk.skala.shopapi.data.table.PointHistory;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.CustomerRepository;
import com.sk.skala.shopapi.repository.PointHistoryRepository;

@SpringBootTest
@Transactional
class CustomerServiceTest {

	@Autowired
	private CustomerService customerService;
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired
	private PointHistoryRepository pointHistoryRepository;

	@MockBean
	private SessionHandler sessionHandler;

	private static final AtomicInteger SEQ = new AtomicInteger();

	private String signUp(String password) {
		Customer customer = new Customer();
		customer.setCustomerId("user" + SEQ.incrementAndGet());
		customer.setCustomerPassword(password);
		customerService.createCustomer(customer);
		return customer.getCustomerId();
	}

	private CustomerSession sessionOf(String id, String password) {
		CustomerSession session = new CustomerSession();
		session.setCustomerId(id);
		session.setCustomerPassword(password);
		return session;
	}

	@Test
	@DisplayName("비밀번호는 평문이 아니라 BCrypt 해시로 저장된다")
	void createCustomer_storesHashedPassword() {
		String id = signUp("pw1234");

		String stored = customerRepository.findById(id).orElseThrow().getCustomerPassword();

		assertThat(stored).isNotEqualTo("pw1234");
		assertThat(stored).startsWith("$2");   // BCrypt 해시 접두사
	}

	@Test
	@DisplayName("회원가입하면 설정된 초기 포인트가 지급된다")
	void createCustomer_grantsInitialPoint() {
		String id = signUp("pw1234");

		assertThat(customerRepository.findById(id).orElseThrow().getCustomerPoint()).isPositive();
	}

	@Test
	@DisplayName("회원가입이 포인트 이력에 SIGNUP 으로 남는다")
	void createCustomer_writesSignupPointHistory() {
		String id = signUp("pw1234");
		long point = customerRepository.findById(id).orElseThrow().getCustomerPoint();

		PointHistory history = pointHistoryRepository
				.findByCustomer_CustomerIdOrderByCreatedAtDesc(id).get(0);

		assertThat(history.getType()).isEqualTo("SIGNUP");
		assertThat(history.getAmount()).isEqualTo(point);
		assertThat(history.getBalance()).isEqualTo(point);
		assertThat(history.getOrderId()).isNull();   // 주문과 무관한 지급
	}

	@Test
	@DisplayName("요청 바디로 포인트나 권한을 지정해도 무시된다")
	void createCustomer_ignoresClientSuppliedPointAndRole() {
		Customer customer = new Customer();
		customer.setCustomerId("hacker" + SEQ.incrementAndGet());
		customer.setCustomerPassword("pw");
		customer.setCustomerPoint(999_999_999L);
		customer.setRole("ADMIN");

		customerService.createCustomer(customer);

		Customer saved = customerRepository.findById(customer.getCustomerId()).orElseThrow();
		assertThat(saved.getCustomerPoint()).isNotEqualTo(999_999_999L);
		assertThat(saved.getRole()).isEqualTo("USER");
	}

	@Test
	@DisplayName("같은 아이디로 다시 가입하면 거부된다")
	void createCustomer_rejectsDuplicateId() {
		String id = signUp("pw1234");

		Customer again = new Customer();
		again.setCustomerId(id);
		again.setCustomerPassword("other");

		assertThatThrownBy(() -> customerService.createCustomer(again))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.DATA_DUPLICATED);
	}

	@Test
	@DisplayName("필수값이 없으면 ParameterException 이 발생한다")
	void createCustomer_requiresIdAndPassword() {
		Customer customer = new Customer();
		customer.setCustomerPassword("pw");

		assertThatThrownBy(() -> customerService.createCustomer(customer))
				.isInstanceOf(ParameterException.class);
	}

	@Test
	@DisplayName("올바른 비밀번호로 로그인하면 응답에 비밀번호가 담기지 않는다")
	void loginCustomer_succeedsAndHidesPassword() {
		String id = signUp("pw1234");

		CustomerDto body = (CustomerDto) customerService.loginCustomer(sessionOf(id, "pw1234")).getBody();

		assertThat(body.getCustomerId()).isEqualTo(id);
	}

	@Test
	@DisplayName("틀린 비밀번호와 없는 아이디는 같은 오류로 응답한다")
	void loginCustomer_doesNotRevealWhichPartIsWrong() {
		String id = signUp("pw1234");

		assertThatThrownBy(() -> customerService.loginCustomer(sessionOf(id, "wrong")))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.NOT_AUTHENTICATED);

		assertThatThrownBy(() -> customerService.loginCustomer(sessionOf("nobody", "pw1234")))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.NOT_AUTHENTICATED);
	}

	@Test
	@DisplayName("비밀번호를 바꾸면 새 비밀번호도 해시로 저장된다")
	void updateCustomer_hashesNewPassword() {
		String id = signUp("pw1234");
		given(sessionHandler.getCustomerId()).willReturn(id);

		Customer request = new Customer();
		request.setCustomerPassword("newpw");
		customerService.updateCustomer(request);

		String stored = customerRepository.findById(id).orElseThrow().getCustomerPassword();
		assertThat(stored).isNotEqualTo("newpw").startsWith("$2");

		// 새 비밀번호로 로그인되고, 옛 비밀번호로는 안 된다.
		customerService.loginCustomer(sessionOf(id, "newpw"));
		assertThatThrownBy(() -> customerService.loginCustomer(sessionOf(id, "pw1234")))
				.isInstanceOf(ResponseException.class);
	}

	@Test
	@DisplayName("다른 사람의 ID 로 수정 요청하면 거부된다")
	void updateCustomer_rejectsOtherAccount() {
		String me = signUp("pw1234");
		String other = signUp("pw1234");
		given(sessionHandler.getCustomerId()).willReturn(me);

		Customer request = new Customer();
		request.setCustomerId(other);
		request.setCustomerName("바꿔치기");

		assertThatThrownBy(() -> customerService.updateCustomer(request))
				.isInstanceOf(ResponseException.class)
				.extracting(e -> ((ResponseException) e).getError())
				.isEqualTo(Error.NOT_AUTHENTICATED);
	}
}
