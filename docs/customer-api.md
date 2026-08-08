# 고객 API 구현 정리

`CustomerService` 전체 구현 내용을 기능 단위로 정리한 문서입니다.

---

## 공통 사항

### 응답 포맷

모든 API는 [Response](../src/main/java/com/sk/skala/shopapi/common/Response.java) 형태로 응답합니다.

```json
// 성공
{ "result": "success", "body": { ... } }

// 실패
{ "result": "fail", "code": "DATA_NOT_FOUND", "message": "데이터를 찾을 수 없습니다." }
```

`@JsonInclude(NON_NULL)` 이 걸려 있어 값이 없는 필드는 응답에서 생략됩니다.

### 인증 방식

로그인 시 JWT를 발급해 **`bff-access` 쿠키**(HttpOnly, 60분)로 내려줍니다. 이후 인증이 필요한 API는 이 쿠키에서 고객 ID를 꺼내 사용합니다.

**요청 바디의 `customerId`는 신뢰하지 않습니다.** 주문·수정·삭제·취소는 모두 쿠키 속 JWT를 기준으로 동작하므로, 남의 계정을 조작할 수 없습니다.

### 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `DATA_NOT_FOUND` | 404 | 데이터를 찾을 수 없음 |
| `DATA_DUPLICATED` | 409 | 이미 존재하는 데이터 (아이디·이메일 중복) |
| `DELETE_NOT_ALLOWED` | 409 | 삭제할 수 없는 데이터 |
| `NOT_AUTHENTICATED` | 401 | 인증 실패 |
| `INSUFFICIENT_FUNDS` | 400 | 보유 포인트 부족 |
| `INSUFFICIENT_QUANTITY` | 400 | 재고 부족 |
| `INVALID_PARAMETER` | 400 | 필수 입력값 누락 |
| `SYSTEM_ERROR` | 500 | 처리되지 않은 예외 |

예외는 서비스에서 던지고 [GlobalExceptionHandler](../src/main/java/com/sk/skala/shopapi/exception/GlobalExceptionHandler.java)가 위 형태로 변환합니다.

---

## 1. 회원가입

`POST /api/customers` · 인증 불필요

### 요청

```json
{
  "customerId": "skala01",
  "customerPassword": "pw1234",
  "customerName": "최인서",
  "email": "skala01@example.com",
  "phone": "010-0000-0000"
}
```

`customerId`, `customerPassword`만 필수. 나머지는 선택입니다.

### 처리 흐름

1. 아이디·비밀번호 공백 검증 → `INVALID_PARAMETER`
2. `existsById`로 아이디 중복 확인 → `DATA_DUPLICATED`
3. 이메일이 입력된 경우에만 중복 확인 → `DATA_DUPLICATED`
4. 기본값 세팅 — 포인트 `0`, 권한 `USER`
5. 저장 후 응답에서 비밀번호를 `null`로 제거

### 핵심 판단

**아이디 중복 확인이 필수입니다.** JPA의 `save()`는 PK가 이미 존재하면 INSERT가 아니라 **UPDATE**로 동작합니다. 이 검증이 없으면 남의 아이디로 가입 요청을 보냈을 때 "가입"이 아니라 **기존 계정의 비밀번호를 덮어쓰는** 결과가 됩니다.

이메일은 `@Column(unique = true)`이므로 미리 확인하지 않으면 DB 제약 위반이 500 에러로 나갑니다. 사용자 입력 문제를 서버 오류로 보이게 하지 않으려고 앞단에서 걸러 409로 응답합니다.

### 알려진 이슈

- `application.yml`의 `app.customer.initial-point: 1000000` 설정을 사용하지 않습니다. 현재는 항상 포인트 0으로 가입됩니다.
- 요청 바디에 `customerPoint`나 `role`을 실어 보내면 **그대로 반영됩니다.** (`"role": "ADMIN"` 으로 관리자 가입 가능) 서버에서 무조건 덮어쓰도록 바꾸는 것이 안전합니다.

---

## 2. 로그인

`POST /api/customers/login` · 인증 불필요

### 요청 / 응답

```json
// 요청
{ "customerId": "skala01", "customerPassword": "pw1234" }

// 응답 body — CustomerDto (비밀번호 필드 자체가 없음)
{ "customerId": "skala01", "customerName": "최인서",
  "email": "skala01@example.com", "phone": "010-0000-0000", "customerPoint": 0 }
```

성공 시 `Set-Cookie: bff-access=<JWT>` 가 함께 내려갑니다.

### 처리 흐름

1. 아이디·비밀번호 공백 검증 → `INVALID_PARAMETER`
2. 고객 조회 실패 → `NOT_AUTHENTICATED`
3. 비밀번호 불일치 → `NOT_AUTHENTICATED`
4. `SessionHandler.storeAccessToken()` 으로 JWT 발급 및 쿠키 저장

### 핵심 판단

**실패 사유를 구분하지 않습니다.** "없는 아이디"와 "틀린 비밀번호"를 모두 같은 `NOT_AUTHENTICATED`로 응답합니다. 구분해서 알려주면 공격자가 어떤 아이디가 존재하는지 알아낼 수 있기 때문입니다.

응답은 엔터티가 아니라 `CustomerDto`를 씁니다. DTO에 비밀번호 필드 자체가 없으므로 실수로 노출될 여지가 없습니다.

### 알려진 이슈

비밀번호를 평문으로 저장·비교합니다. BCrypt 적용 시 회원가입·로그인·정보수정·탈퇴 네 곳을 함께 수정해야 하며, 비교는 반드시 `equals()`가 아닌 `matches()`를 써야 합니다.

---

## 3. 고객 목록 조회

`GET /api/customers?offset=0&count=10` · 인증 불필요

### 응답

```json
{ "result": "success",
  "body": { "total": 3, "offset": 0, "count": 10, "list": [ ... ] } }
```

### 처리 흐름

1. `PageRequest.of(offset, count)` 로 페이징 조회
2. 각 고객의 비밀번호를 `null`로 제거
3. `PagedList`로 감싸 응답

### 알려진 이슈

엔터티를 그대로 응답하므로 비밀번호를 수동으로 지워야 합니다. `CustomerDto`로 변환하면 이 처리가 필요 없어집니다. 또한 `@Transactional`이 없어 `null` 세팅이 DB에 반영되지 않는데, 만약 트랜잭션을 붙이면 **더티 체킹으로 실제 비밀번호가 지워지므로** 반드시 DTO 변환으로 바꿔야 합니다.

---

## 4. 고객 상세 + 주문 상품 목록

`GET /api/customers/{customerId}` · 인증 불필요

### 응답

```json
{
  "customerId": "skala01",
  "customerPoint": 994000.0,
  "products": [
    { "productId": 1, "productName": "말랑이", "unitPrice": 3000,
      "quantity": 2, "subtotal": 6000, "itemStatus": "ORDERED" }
  ]
}
```

### 처리 흐름

1. 고객 조회 → 없으면 `DATA_NOT_FOUND`
2. `findByOrders_Customer_CustomerId` 로 해당 고객의 주문 상세 전체 조회
3. `OrderItem` → `OrderItemDto` 변환
4. `OrderListDto` 조립 후 응답

### 핵심 판단

**`@Transactional(readOnly = true)` 가 필요합니다.** `OrderItem.product`가 `FetchType.LAZY`라 트랜잭션 밖에서 접근하면 `LazyInitializationException`이 발생합니다.

주문이 없는 고객은 에러가 아니라 **빈 배열**로 응답합니다.

상품이 삭제된 주문 이력에 대비해 `productId`를 null-safe하게 처리했습니다. 상품명·단가는 `OrderItem`에 주문 시점 스냅샷으로 남아 있어 상품이 사라져도 내역이 온전히 보입니다.

### 알려진 이슈

`OrderListDto.customerPoint`가 `Double`이라 `Long`에서 변환이 필요합니다. 포인트에 소수점은 의미가 없으므로 DTO를 `Long`으로 바꾸는 편이 자연스럽습니다.

---

## 5. 고객 정보 수정

`PUT /api/customers` · **인증 필요**

### 요청

```json
{ "customerName": "최인서", "phone": "010-1111-2222", "email": "new@example.com" }
```

값을 보낸 항목만 수정됩니다.

### 처리 흐름

1. 쿠키에서 로그인한 고객 ID 확보
2. 바디에 다른 사람의 `customerId`가 실려 있으면 → `NOT_AUTHENTICATED`
3. 세션 기준으로 고객 조회
4. 이메일이 **변경된 경우에만** 중복 확인 → `DATA_DUPLICATED`
5. 값이 들어온 항목만 반영 후 `CustomerDto`로 응답

### 핵심 판단

**수정 대상에서 제외한 필드가 있습니다** — `customerId`(PK), `customerPoint`, `role`. 포인트나 권한을 클라이언트가 바꿀 수 있으면 안 되기 때문입니다.

이메일 중복 확인은 값이 실제로 바뀔 때만 수행합니다. 그렇지 않으면 자기 자신의 이메일에 걸려 항상 409가 납니다.

`@Transactional` 안에서 조회한 엔터티를 수정하므로 **`save()` 호출 없이** 더티 체킹으로 UPDATE가 나갑니다.

---

## 6. 회원 탈퇴

`DELETE /api/customers` · **인증 필요**

### 요청

```json
{ "customerPassword": "pw1234" }
```

### 처리 흐름

1. 쿠키에서 고객 ID 확보 후 조회
2. 비밀번호 재확인 → 불일치 시 `NOT_AUTHENTICATED`
3. **주문 이력이 있으면 삭제 거부** → `DELETE_NOT_ALLOWED`
4. 삭제 후 쿠키 제거

### 핵심 판단

주문 이력이 있는 고객을 물리 삭제하면 `orders.customer_id` FK 제약에 걸리거나 주문 내역이 고아가 됩니다. 실무에서도 "이력이 있으면 삭제 불가"가 일반적이라 이 정책을 택했습니다.

삭제 같은 되돌릴 수 없는 작업에는 비밀번호 재확인을 넣었습니다.

### 대안

`Customer`에 `status` 필드를 추가해 소프트 삭제(탈퇴 표시만)로 바꾸면 주문 이력이 있어도 탈퇴가 가능합니다. 이 경우 조회 API들이 탈퇴 회원을 걸러내도록 함께 수정해야 합니다.

---

## 7. 상품 주문

`POST /api/customers/order` · **인증 필요**

### 요청

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 5, "quantity": 1 }
  ],
  "receiverName": "최인서",
  "receiverPhone": "010-0000-0000",
  "zipcode": "06234",
  "address1": "서울시 강남구 ...",
  "address2": "101동 202호",
  "deliveryMemo": "문 앞에 놓아주세요"
}
```

### 응답 — `OrderDto`

```json
{
  "orderId": 1, "orderNumber": "KK20260808143022", "customerId": "skala01",
  "totalAmount": 9000, "status": "PAID", "receiverName": "최인서",
  "address1": "서울시 강남구 ...", "address2": "101동 202호",
  "orderedAt": "2026-08-08T14:30:22",
  "items": [ { "productId": 1, "productName": "말랑이", "unitPrice": 3000,
               "quantity": 2, "subtotal": 6000, "itemStatus": "ORDERED" } ]
}
```

### 처리 흐름

1. 쿠키에서 고객 ID 확보
2. 고객 조회 → `DATA_NOT_FOUND`
3. 주문 상품 목록이 비었는지 검증 → `INVALID_PARAMETER`
4. `Orders` 생성 + 배송지 정보 세팅
5. 상품별 반복 처리
   - 상품 조회 → `DATA_NOT_FOUND`
   - 수량 검증 (null·0 이하) → `INVALID_PARAMETER`
   - 재고 차감 실패 → `INSUFFICIENT_QUANTITY`
   - `OrderItem` 생성 후 주문에 연결, 소계를 총액에 누적
6. 총액만큼 포인트 차감, 실패 시 → `INSUFFICIENT_FUNDS`
7. 주문 저장 (상세는 cascade로 함께 저장)
8. `OrderDto` 조립 후 응답

### 핵심 판단

**`@Transactional`이 필수입니다.** 재고를 먼저 깎고 나중에 포인트가 부족해 실패하면, 트랜잭션이 없을 때 재고만 깎인 상태로 남습니다. 예외 발생 시 전부 롤백되어야 합니다.

**포인트는 상품별이 아니라 총액으로 한 번에 차감합니다.** 상품별로 깎으면 중간에 잔액이 떨어졌을 때 앞 상품만 결제된 어중간한 상태가 됩니다.

**수량 검증에서 음수를 반드시 막아야 합니다.** `quantity: -5`가 통과하면 재고가 오히려 늘고 포인트도 증가합니다.

**주문 시점 정보를 스냅샷으로 남깁니다.** 배송지는 `Orders`에, 상품명·단가는 `OrderItem`에 복사됩니다. 고객이 이사하거나 상품 가격이 바뀌어도 과거 주문 내역은 그대로 유지됩니다.

**저장은 `ordersRepository.save()` 한 번뿐입니다.** 주문 상세는 `cascade = ALL`로 함께 저장되고, 고객 포인트와 상품 재고는 더티 체킹으로 자동 반영됩니다. `orderItemRepository.save()`나 `customerRepository.save()`는 부르지 않습니다.

`orderNumber`(`KK` + 날짜시각)와 `orderedAt`은 `@PrePersist`가 저장 직전에 생성합니다.

### 알려진 이슈

포인트 이력(`PointHistory`)을 남기지 않습니다. 잔액 변동의 근거가 남지 않으므로 추후 추가가 필요합니다 (`type: "ORDER_USE"`, `amount`는 음수).

---

## 8. 주문 취소

`POST /api/customers/cancel` · **인증 필요**

### 요청

```json
// 주문 전체 취소
{ "orderId": 1 }

// 특정 상품만 취소
{ "orderId": 1, "productId": 5, "reasonCode": "CHANGE_MIND" }
```

`productId`가 `null`이면 주문 전체, 값이 있으면 해당 상품 항목만 취소합니다.

### 처리 흐름

1. 쿠키에서 고객 ID 확보 후 고객 조회
2. `orderId` 누락 → `INVALID_PARAMETER`
3. 주문 조회 → `DATA_NOT_FOUND`
4. **본인 주문인지 확인** → 아니면 `NOT_AUTHENTICATED`
5. 취소 대상 선별 (전체 / 특정 상품)
6. 대상별로 상태를 `CANCELED`로 바꾸고 재고 복구, 환급액 누적
7. 취소된 항목이 하나도 없으면 → `DATA_NOT_FOUND` ("이미 취소된 주문입니다")
8. 포인트 환급 + 주문 총액 차감
9. 모든 항목이 취소되면 주문 상태도 `CANCELED`로 변경
10. `OrderDto`로 응답

### 핵심 판단

**소유권 검증이 핵심입니다.** `orderId`만으로 취소하면 남의 주문 번호를 넣어 취소할 수 있으므로, 주문의 주인과 로그인한 고객이 같은지 반드시 확인합니다.

**이미 취소된 항목은 건너뜁니다.** 그렇지 않으면 같은 요청을 두 번 보내 포인트를 중복 환급받을 수 있습니다.

부분 취소 시 주문 총액을 환급액만큼 줄이고, 남은 항목이 없을 때만 주문 전체를 `CANCELED`로 바꿉니다.

### 알려진 이슈

- **컨트롤러 시그니처를 변경했습니다.** 기존에는 `OrderRequest`를 받았으나 그 DTO에는 `orderId`가 없어 어떤 주문을 취소할지 특정할 수 없었습니다. `CancelRequest`로 교체했습니다.
- **수량 단위 부분 취소는 미지원입니다.** `CancelRequest.quantity`를 사용하지 않으며, 취소는 상품 항목 단위로만 이뤄집니다. "3개 중 1개만 취소"를 지원하려면 `OrderItem`을 분할하는 로직이 필요합니다.
- 포인트 이력을 남기지 않습니다 (`type: "ORDER_REFUND"`).

---

## 공통 헬퍼

DTO 변환이 여러 곳에서 반복되어 private 메서드로 분리했습니다.

| 메서드 | 역할 |
|---|---|
| `toOrderItemDtos(List<OrderItem>)` | 주문 상세 목록 → `List<OrderItemDto>` |
| `toOrderDto(Orders)` | 주문 1건 → `OrderDto` (상세 목록 포함) |
| `toCustomerDto(Customer)` | 고객 → `CustomerDto` (비밀번호 제외) |

`getCustomerById`와 `placeOrder`는 단계별 구현 흔적을 남겨두기 위해 아직 헬퍼를 쓰지 않고 인라인 코드를 유지하고 있습니다.

---

## 전체 남은 과제

| 항목 | 내용 |
|---|---|
| 비밀번호 암호화 | 전 구간 평문 저장. BCrypt 적용 필요 |
| 포인트 이력 | `PointHistory` 미기록 (주문·취소·가입 모두) |
| 초기 포인트 | `app.customer.initial-point` 설정 미사용 |
| 권한 상승 | 회원가입 시 `role`을 클라이언트가 지정 가능 |
| 수량 부분 취소 | 상품 항목 단위까지만 지원 |
| `ProductService` | 5개 메서드 전부 미구현 |
