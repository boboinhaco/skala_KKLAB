# skala-shop-api

스칼라 온라인 쇼핑몰 백엔드 REST API 실습 프로젝트

## 기술 스택

Spring Boot 3.3.0 · Java 17 · Gradle · Spring Data JPA · H2(in-memory) · JWT(jjwt 0.11.5) · Lombok · AOP · Actuator

## 시작하기

```bash
# 1) Gradle Wrapper 생성 (최초 1회, gradle이 설치되어 있어야 함)
gradle wrapper --gradle-version 8.8

# 2) 빌드
./gradlew build

# 3) 실행
./gradlew bootRun
```

- API: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:shopdb`, User: `sa`, Password 없음)
- Actuator: http://localhost:8080/actuator/health

> Gradle이 없으면 IntelliJ에서 `build.gradle`을 열어 Import 하면 IDE가 자동으로 처리합니다.

## 패키지 구조

```
com.sk.skala.shopapi
├─ ShopApiApplication.java      # @SpringBootApplication 메인 클래스
├─ controller/                  # REST API 진입점
│  ├─ ProductController.java    # ✅ 완성
│  └─ CustomerController.java   # ✅ 완성
├─ service/                     # 비즈니스 로직
│  ├─ ProductService.java       # ⬜ TODO - 직접 구현
│  └─ CustomerService.java      # ⬜ TODO - 직접 구현
├─ repository/                  # JPA 데이터 접근
│  ├─ ProductRepository.java    # ✅ 완성
│  ├─ CustomerRepository.java   # ✅ 완성
│  └─ OrderItemRepository.java  # ✅ 완성
├─ data/
│  ├─ table/                    # JPA 엔터티
│  │  ├─ Product.java           # ✅ 완성
│  │  ├─ Customer.java          # ✅ 완성
│  │  └─ OrderItem.java         # ✅ 완성
│  └─ dto/                      # 요청·응답 객체
│     ├─ OrderItemDto.java      # ✅ 완성
│     ├─ OrderListDto.java      # ✅ 완성
│     ├─ OrderRequest.java      # ✅ 완성
│     └─ CustomerSession.java   # ✅ 완성
├─ common/                      # 횡단 관심사
│  ├─ Response.java             # ✅ 공통 응답 포맷
│  ├─ PagedList.java            # ✅ 페이징 결과
│  └─ SessionHandler.java       # ✅ JWT 발급·검증
├─ exception/
│  ├─ Error.java                # ✅ 에러 코드 enum
│  ├─ ResponseException.java    # ✅ 비즈니스 예외
│  ├─ ParameterException.java   # ✅ 입력 검증 예외
│  └─ GlobalExceptionHandler.java # ✅ 전역 예외 처리
├─ aop/
│  └─ ApiLoggingAspect.java     # ✅ API 호출 로깅
└─ tools/
   └─ StringUtil.java           # ✅ 문자열 유틸
```

## 구현 순서

1. **Entity** → 이미 작성됨. `Product`, `Customer`, `OrderItem` 관계 먼저 이해하기
2. **Repository** → 이미 작성됨. 쿼리 메서드 작명 규칙 확인
3. **Service** → **여기가 실습 핵심.** 각 메서드의 `TODO` 주석을 따라 구현
4. **Controller** → 이미 작성됨. 서비스 완성되면 바로 동작

`ProductService` → `CustomerService`(회원가입/로그인) → `placeOrder` / `cancelOrder` 순서를 추천합니다.

## API 목록

### 상품 (Product)

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/products?offset=0&count=10` | 상품 전체 목록 (페이징) |
| GET | `/api/products/{id}` | 상품 상세 조회 |
| POST | `/api/products` | 상품 등록 |
| PUT | `/api/products` | 상품 정보 수정 |
| DELETE | `/api/products` | 상품 삭제 |

### 고객 (Customer)

| Method | URI | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/customers?offset=0&count=10` | 고객 전체 목록 (페이징) | - |
| GET | `/api/customers/{customerId}` | 고객 상세 + 주문 목록 | - |
| POST | `/api/customers` | 회원가입 (초기 포인트 지급) | - |
| POST | `/api/customers/login` | 로그인 (JWT 쿠키 발급) | - |
| PUT | `/api/customers` | 고객 정보 수정 | - |
| DELETE | `/api/customers` | 고객 삭제 | - |
| POST | `/api/customers/order` | 상품 주문 (포인트 차감) | JWT |
| POST | `/api/customers/cancel` | 주문 취소 (포인트 환급) | JWT |

## 비즈니스 규칙

| 구분 | 규칙 |
|---|---|
| 포인트 | 보유 포인트로만 주문 가능, 부족 시 `INSUFFICIENT_FUNDS` |
| 주문 수량 | 같은 상품 재주문 시 수량 누적, 취소 시 차감 (0이면 삭제) |
| 인증 | 주문·취소는 로그인 필수, 쿠키(`bff-access`)의 JWT로 고객 식별 |
| 입력 검증 | 필수값 누락 시 `ParameterException` |
| 예외 처리 | 없는 데이터 조회 시 `DATA_NOT_FOUND` (전역 예외 처리) |
| 트랜잭션 | 주문·취소는 `@Transactional`로 원자적 처리 |

## 테스트 시나리오 (curl)

```bash
# 1) 회원가입
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}'

# 2) 로그인 (쿠키 저장)
curl -X POST http://localhost:8080/api/customers/login \
  -H "Content-Type: application/json" \
  -c cookie.txt \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}'

# 3) 상품 목록 조회
curl http://localhost:8080/api/products

# 4) 상품 주문 (쿠키 첨부)
curl -X POST http://localhost:8080/api/customers/order \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"productId":1,"quantity":2}'

# 5) 내 주문 확인
curl http://localhost:8080/api/customers/skala01

# 6) 주문 취소
curl -X POST http://localhost:8080/api/customers/cancel \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"productId":1,"quantity":1}'
```

## Docker 배포

```bash
./gradlew build
docker build -t shop-api:1.0 .
docker run -p 8080:8080 shop-api:1.0
```

> Apple Silicon(M1/M5)에서 빌드한 이미지를 x86 서버에 올릴 때는 `--platform linux/amd64` 옵션을 붙이세요.
