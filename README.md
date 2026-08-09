# 말랑연구소 (skala-shop-api)

SKALA 실습 과제 **스칼라 온라인 쇼핑몰 API**를 구현하고, 그 위에 말랑이(스퀴시) 쇼핑몰 프론트엔드를 올린 프로젝트입니다.

- 백엔드 : Spring Boot 3.3.0 · Java 17 · Spring Data JPA · H2(in-memory) · JWT(jjwt 0.11.5) · AOP · Actuator · Gradle
- 프론트 : 정적 HTML/CSS/JS (`src/main/resources/static`) — Spring Boot가 그대로 서빙

```bash
./gradlew bootRun
```

| 주소 | 설명 |
|---|---|
| http://localhost:8080 | 말랑연구소 쇼핑몰 화면 |
| http://localhost:8080/h2-console | H2 콘솔 (JDBC `jdbc:h2:mem:shopdb`, User `sa`, PW 없음) |
| http://localhost:8080/actuator/health | 헬스 체크 |

> 포트 충돌 시 : `kill $(lsof -t -nP -iTCP:8080 -sTCP:LISTEN)`

---

## 1. 실습 요구사항 충족 현황

실행해서 응답 코드까지 확인한 결과입니다.

### 1-1. API 목록 (실습 자료 p.11)

| Method | URI | 요구 | 구현 | 확인 |
|---|---|:--:|:--:|:--:|
| GET | `/api/products` | O | O | 200 |
| GET | `/api/products/{id}` | O | O | 200 |
| POST | `/api/products` | O | O | 200 |
| PUT | `/api/products` | O | O | 200 |
| DELETE | `/api/products` | O | O | 200 |
| GET | `/api/customers` | O | O | 200 |
| GET | `/api/customers/{customerId}` | O | O | 200 |
| GET | `/api/customers/name/{customerName}` | O | O | 200 |
| POST | `/api/customers` | O | O | 200 |
| POST | `/api/customers/login` | O | O | 200 |
| PUT | `/api/customers` | O | O | 200 |
| DELETE | `/api/customers/{customerId}` | O | O | 200 |
| GET | `/api/customers/{customerId}/products` | O | O | 200 |
| POST | `/api/customers/order` | O | O | 200 |
| POST | `/api/customers/cancel` | O | O | 200 |

**이름 조회 경로를 바꾼 이유** — 자료에는 `GET /api/customers/{customerName}` 으로 되어 있는데, 바로 위의 `{customerId}` 와 경로 패턴이 완전히 같아 Spring이 둘을 구분할 수 없습니다. `/api/customers/name/{customerName}` 으로 분리했고, 동명이인을 고려해 **목록**으로 반환합니다.

### 1-2. 고객 주문 여정 (p.3)

| 단계 | API | 확인 |
|---|---|:--:|
| 1. 회원가입 (초기 포인트 지급) | `POST /api/customers` | 통과 |
| 2. 로그인 (JWT 쿠키 발급) | `POST /api/customers/login` | 통과 |
| 3. 상품 조회 | `GET /api/products` | 통과 |
| 4. 상품 주문 (포인트 차감) | `POST /api/customers/order` | 통과 |
| 5. 주문 확인 | `GET /api/customers/{id}` | 통과 |
| 6. 주문 취소 (포인트 환급) | `POST /api/customers/cancel` | 통과 |

### 1-3. 비즈니스 규칙 & 예외 처리 (p.4)

| 규칙 | 구현 위치 | 확인 |
|---|---|:--:|
| 포인트 부족 시 주문 거부 → `INSUFFICIENT_FUNDS` | `Customer.usePoint()` + `placeOrder` | 통과 |
| 재고 부족 시 → `INSUFFICIENT_QUANTITY` | `Product.decreaseStock()` + `placeOrder` | 통과 |
| 주문·취소는 로그인 필수 (Cookie JWT로 식별) | `SessionHandler.getCustomerId()` | 통과 |
| 필수값 검증 실패 → `ParameterException` (`INVALID_PARAMETER`) | `StringUtil.isAnyEmpty()` | 통과 |
| 없는 데이터 조회 → `DATA_NOT_FOUND` (전역 예외 처리) | `GlobalExceptionHandler` | 통과 |
| 주문·취소는 `@Transactional` 로 원자적 처리 | `placeOrder` / `cancelOrder` | 적용 |

### 1-4. 계층 구조 · 패키지 (p.9, p.32)

```
com.sk.skala.shopapi
├─ controller/   ProductController · CustomerController
├─ service/      ProductService · CustomerService · OrderService
├─ repository/   Product · Customer · OrderItem · Orders · PointHistory Repository
├─ data/
│  ├─ table/     Product · Customer · OrderItem · Orders · PointHistory
│  └─ dto/       ProductDto · OrderItemDto · OrderListDto · OrderDto
│                CustomerDto · CustomerSession · OrderRequest · CancelRequest
├─ common/       Response · PagedList · SessionHandler(JWT)
├─ exception/    Error · ResponseException · ParameterException · GlobalExceptionHandler
├─ aop/          ApiLoggingAspect
└─ tools/        StringUtil
```

`Controller(요청) → Service(로직) → Repository(JPA) → DB` 계층 구조를 따릅니다.

서비스는 도메인 단위로 나눴습니다.

| 서비스 | 책임 |
|---|---|
| `CustomerService` | 회원가입 · 로그인 · 조회 · 수정 · 탈퇴 |
| `OrderService` | 주문 · 주문 목록 · 취소 (재고/포인트 트랜잭션) |
| `ProductService` | 상품 CRUD |

엔터티 → DTO 변환은 서비스가 아니라 **DTO 의 정적 팩토리**(`OrderDto.from(...)`, `CustomerDto.from(...)`)가 담당합니다. 같은 변환을 여러 서비스에서 쓰기 때문입니다.

---

## 2. 실습 자료와 다르게 구현한 부분

과제 시작 시 제공된 프로젝트가 자료보다 확장된 형태였고, 그 설계를 유지했습니다. **의도적인 차이**이므로 근거와 함께 정리합니다.

### 2-1. 주문 구조 : 주문서(Orders) 도입

| | 실습 자료 | 이 프로젝트 |
|---|---|---|
| 엔터티 | `OrderItem(customer, product, quantity)` | `Orders(주문서)` 1 : N `OrderItem(주문상세)` |
| 재주문 | 같은 상품이면 **수량 누적** | 주문할 때마다 **새 주문서 생성** |
| 취소 | 수량 차감, 0이면 삭제 | 항목 상태를 `CANCELED` 로 변경 (이력 보존) |

**왜** — 주문 번호·주문 일시·배송지가 있어야 실제 쇼핑몰로 동작합니다. 수량 누적 방식은 "언제 몇 개를 어디로 주문했는지"가 남지 않아 주문 내역을 만들 수 없습니다. 취소도 삭제 대신 상태 변경으로 처리해 이력을 남깁니다.

### 2-2. 스냅샷 보관

`OrderItem` 은 주문 시점의 `productName` · `unitPrice` 를, `Orders` 는 주문 시점의 배송지를 **복사해서** 보관합니다. 상품 가격이 바뀌거나 고객이 이사해도 과거 주문 내역이 변하지 않습니다.

### 2-3. 타입

| 필드 | 자료 | 이 프로젝트 | 이유 |
|---|---|---|---|
| `Product.productPrice` | `Double` | `Long` | 금액은 소수점이 필요 없고, `Double` 은 부동소수점 오차가 생김 |
| `Customer.customerPoint` | `Double` | `Long` | 위와 동일 |
| `OrderListDto.customerPoint` | `Double` | `Double` (유지) | 자료의 응답 형식을 지키기 위해 DTO 에서만 변환 |

### 2-4. 컨트롤러 매핑

자료 p.28/p.30 에는 목록 조회가 `@GetMapping("/list")` 로 되어 있으나, 같은 자료 p.11 의 API 표에는 `/api/products` · `/api/customers` 로 되어 있습니다. **자료 안에서 서로 다릅니다.** REST 관례상 컬렉션 조회는 리소스 경로 자체를 쓰는 것이 맞아 p.11 을 따랐습니다.

---

## 3. 추가로 구현한 기능

과제 범위를 넘어 직접 추가한 부분입니다.

### 3-1. 백엔드

| 기능 | 내용 |
|---|---|
| 상품 확장 속성 | `categoryId` · `description` · `texture`(촉감) · `soundLevel`(소리 1~5) · `stretchLevel`(늘어남 1~5) · `scent`(향) · `stockQuantity` · `status` |
| 판매 지표 | `salesCount` · `reviewCount` · `likeCount` — 인기순/판매순/후기순 정렬의 근거 데이터 |
| 재고 관리 | 주문 시 차감, 취소 시 복구. 부족하면 `INSUFFICIENT_QUANTITY` |
| 다중 상품 주문 | `OrderRequest.items[]` 로 여러 상품을 한 번에 주문 |
| 부분 취소 | `CancelRequest.productId` 가 있으면 그 상품만, 없으면 주문 전체 취소 |
| 주문 목록 조회 | `GET /api/customers/orders` — 로그인한 고객의 주문을 최신순으로, `join fetch` 로 N+1 방지 |
| 소유권 검증 | 주문·취소·수정·삭제는 요청 바디가 아닌 **JWT 의 고객 ID** 기준으로만 동작 |
| 삭제 제약 | 주문 이력이 있는 고객·상품은 삭제 거부 (`DELETE_NOT_ALLOWED`, 409) |
| 초기 포인트 | `app.customer.initial-point` 설정값을 회원가입 시 지급 |
| 포인트 이력 | 가입(`SIGNUP`)·주문(`ORDER_USE`)·취소(`ORDER_REFUND`) 시 `point_history` 에 변동액과 처리 후 잔액을 기록 |
| 이메일 중복 확인 | `@Column(unique = true)` 위반이 500 으로 나가지 않도록 사전 검사 |

추가한 에러 코드 : `DELETE_NOT_ALLOWED(409)`

### 3-2. 프론트엔드 (`/`)

Y2K 감성 + 연구소 컨셉의 정적 페이지. 모든 데이터는 위 REST API 를 호출해 렌더링합니다.

| 영역 | 기능 |
|---|---|
| 히어로 | CSS 로 만든 레트로 계측기 + SVG 애니메이션 루프 (`/img/hero.mp4` 를 넣으면 자동으로 영상 교체) |
| 연구노트 | 표본 수·카테고리 수·리포트 수를 API 응답으로 계산해 표시 |
| 주문방법 | 4단계 안내, 각 단계에서 회원가입/카탈로그/장바구니로 바로 이동 |
| 카테고리 | 만들기 재료 · 슬랑이 · 크런치 슬랑이 · 말랑이 (`categoryId` 기준 필터) |
| 정렬 | 인기순 · 판매순 · 후기순 |
| 상품 상세 | 배경 블러 팝업, 4컷 갤러리(FRONT/SIDE/MACRO/SQUISHED), 계측 리포트 표 |
| 장바구니 | 화면 우측 중앙 고정 메뉴 — 포인트 · 장바구니 · 주문내역 |
| 주문내역 | 주문번호·일시·상태·상품 목록, **상품별 취소 / 주문 전체 취소** |

---

## 4. 실행 중 발견하고 고친 버그

### 4-1. 같은 초에 주문 2건이 들어오면 두 번째가 500

`Orders.orderNumber` 가 `KK + yyyyMMddHHmmss`(초 단위)인데 `@Column(unique = true)` 라, 동시 주문 시 번호가 충돌했습니다.

```java
// 수정 후 : 밀리초 + 난수 4자리
"KK" + LocalDateTime.now().format(ORDER_NUMBER_FORMAT)   // yyyyMMddHHmmssSSS
     + String.format("%04d", ThreadLocalRandom.current().nextInt(10000))
```

동시 요청 2건이 서로 다른 번호를 받는 것까지 확인했습니다.

### 4-2. 초기 포인트 설정이 코드에 연결되지 않음

`application.yml` 에 `app.customer.initial-point: 1000000` 이 있는데 회원가입은 포인트 0으로 만들고 있어, **가입 직후 아무것도 주문할 수 없었습니다.** `@Value` 로 연결했고, 요청 바디의 `customerPoint` 는 무시하도록 했습니다(클라이언트가 포인트를 정하지 못하게).

### 4-3. 시드 데이터가 NOT NULL 컬럼을 채우지 않음

`data.sql` 이 `stock_quantity` · `status` 를 넣지 않았습니다. 말랑이 상품 14종으로 교체하면서 모든 필수 컬럼을 채웠습니다.

### 4-4. 동시 주문 시 포인트·재고 차감 유실 (lost update)

두 주문이 동시에 들어오면 각자 읽은 값에서 각자 차감해 나중 커밋이 앞선 차감을 덮어썼습니다. 순차 주문은 정상이라 눈에 띄지 않는 종류의 버그입니다.

```java
// CustomerRepository / ProductRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from Customer c where c.customerId = :customerId")
Optional<Customer> findByIdForUpdate(@Param("customerId") String customerId);
```

주문·취소에서 포인트와 재고를 읽을 때 이 메서드를 씁니다. 여러 상품을 주문할 때는 **상품 ID 오름차순으로 잠가** 교착을 피합니다.

검증 : 10건 동시 주문 → 포인트 970,000 (= 1,000,000 − 3,000×10), 재고 45 → 35 정확히 일치.

### 4-5. 비밀번호 평문 저장

`spring-security-crypto` 만 추가해 BCrypt 를 적용했습니다. `spring-boot-starter-security` 는 모든 요청에 인증 필터를 걸어 별도 설정이 필요하므로 쓰지 않았습니다.

- 저장 : 회원가입 · 비밀번호 변경 시 `passwordEncoder.encode(...)`
- 검증 : 로그인 · 탈퇴 시 `passwordEncoder.matches(...)`

BCrypt 는 솔트가 해시에 포함되어 같은 비밀번호도 매번 다른 값이 됩니다. 그래서 `equals()` 비교는 절대 성립하지 않습니다.

---

## 5. 남은 과제

| 항목 | 내용 |
|---|---|
| 수량 단위 부분 취소 | 취소는 상품 항목 단위까지만 지원합니다. "3개 중 1개 취소"는 `OrderItem` 분할이 필요합니다. |
| 클라이언트 오류가 500 으로 응답 | `GlobalExceptionHandler` 의 `Exception` 핸들러가 Spring MVC 예외까지 잡아, 잘못된 Content-Type 이 415 가 아닌 500 으로 나갑니다. |

---

## 6. 공통 응답 형식

```json
// 성공
{ "result": "success", "body": { ... } }

// 실패
{ "result": "fail", "code": "DATA_NOT_FOUND", "message": "데이터를 찾을 수 없습니다." }
```

| 코드 | HTTP | 의미 |
|---|:--:|---|
| `DATA_NOT_FOUND` | 404 | 데이터 없음 |
| `DATA_DUPLICATED` | 409 | 아이디·이메일·상품명 중복 |
| `DELETE_NOT_ALLOWED` | 409 | 주문 이력이 있어 삭제 불가 |
| `NOT_AUTHENTICATED` | 401 | 로그인 필요 / 인증 실패 |
| `INSUFFICIENT_FUNDS` | 400 | 포인트 부족 |
| `INSUFFICIENT_QUANTITY` | 400 | 재고 부족 |
| `INVALID_PARAMETER` | 400 | 필수값 누락 |
| `SYSTEM_ERROR` | 500 | 처리되지 않은 예외 |

인증은 로그인 시 발급되는 **`bff-access` 쿠키(HttpOnly, 60분)** 의 JWT 로 이뤄집니다.

---

## 7. 테스트

```bash
./gradlew test
```

| 클래스 | 건수 | 검증 대상 |
|---|:--:|---|
| `OrderServiceTest` | 9 | 포인트·재고 차감, 실패 시 롤백, 취소 환급, 중복 취소 차단, 소유권 검증, **동시성** |
| `CustomerServiceTest` | 10 | BCrypt 해시 저장·검증, 초기 포인트, 포인트·권한 조작 차단, 중복 아이디, 타인 계정 수정 차단 |
| `ShopApiApplicationTests` | 1 | 컨텍스트 로딩 |

주문·취소 테스트는 **롤백과 동시성을 실제로 보기 위해 `@Transactional` 을 붙이지 않고** 커밋이 일어나게 했습니다.

동시성 테스트는 8개 스레드가 동시에 주문을 넣고 최종 잔액을 확인합니다. 잠금을 제거하면 이 테스트만 실패하는 것을 확인했습니다 — 즉 잠금이 실제로 동작하는지 검증합니다.

```java
// 잠금이 없으면 차감이 유실되어 잔액이 10,000 에 가깝게 남는다.
assertThat(pointOf(customer)).isEqualTo(10_000L - (threads * 1_000L));
```

## 8. 테스트 시나리오 (curl)

```bash
# 1) 회원가입 - 초기 포인트 1,000,000P 지급
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala01","customerPassword":"pw1234","customerName":"스칼라"}'

# 2) 로그인 - JWT 쿠키 저장
curl -X POST http://localhost:8080/api/customers/login \
  -H "Content-Type: application/json" -c cookie.txt \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}'

# 3) 상품 목록
curl "http://localhost:8080/api/products?offset=0&count=10"

# 4) 주문 - 여러 상품을 한 번에
curl -X POST http://localhost:8080/api/customers/order \
  -H "Content-Type: application/json" -b cookie.txt \
  -d '{"items":[{"productId":1,"quantity":2},{"productId":5,"quantity":1}],
       "receiverName":"스칼라","address1":"서울시 강남구"}'

# 5) 내 주문 확인
curl http://localhost:8080/api/customers/skala01
curl -b cookie.txt http://localhost:8080/api/customers/orders   # 주문 단위 조회

# 6-1) 상품 하나만 취소
curl -X POST http://localhost:8080/api/customers/cancel \
  -H "Content-Type: application/json" -b cookie.txt \
  -d '{"orderId":1,"productId":5}'

# 6-2) 주문 전체 취소
curl -X POST http://localhost:8080/api/customers/cancel \
  -H "Content-Type: application/json" -b cookie.txt \
  -d '{"orderId":1}'
```

---

## 9. 초기 시드 데이터

`data.sql` — 말랑이 상품 14종 (카테고리별로 재료 4 / 슬랑이 3 / 크런치 슬랑이 3 / 말랑이 4)

| categoryId | 카테고리 | 예시 |
|:--:|---|---|
| 1 | 만들기 재료 | 반짝 글리터 세트, 폼비즈 컬러믹스, 슬라임 활성화제, 파스텔 색소 4종 |
| 2 | 슬랑이 | 쭈욱 딸기 슬랑이, 말차 라떼 슬랑이, 우주 반짝 슬랑이 |
| 3 | 크런치 슬랑이 | 크런치 구름빵, 콘프레이크 크런치(재고 0 — 품절 표시 확인용), 팝콘 버터 크런치 |
| 4 | 말랑이 | 복숭아 젤리 말랑이, 버터 토끼 스퀴시, 말랑 푸딩 볼, 솜사탕 구름 쿠션 |

`ddl-auto: create-drop` 이라 **재시작할 때마다 초기화**됩니다.

---

## 10. Docker 배포

```bash
./gradlew build
docker build -t shop-api:1.0 .
docker run -p 8080:8080 shop-api:1.0
```

> Apple Silicon 에서 빌드한 이미지를 x86 서버에 올릴 때는 `--platform linux/amd64`
