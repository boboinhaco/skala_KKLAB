-- 초기 상품 시드 데이터
-- category_id : 1=만들기 재료  2=슬랑이  3=크런치 슬랑이  4=말랑이
-- stock_quantity, status 는 NOT NULL 컬럼이므로 반드시 값을 넣어야 합니다.

-- 1. 만들기 재료 ------------------------------------------------------------
INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (1, '반짝 글리터 세트', 2500, '5색 글리터로 우주 슬라임 만들기', NULL, NULL, NULL, NULL, 60, 'ON_SALE', 320, 88, 240);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (1, '폼비즈 컬러믹스', 3000, '알록달록 폼비즈 대용량 200g', NULL, NULL, NULL, NULL, 45, 'ON_SALE', 410, 132, 315);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (1, '슬라임 활성화제', 2000, '실패 없는 농도 조절 필수템', NULL, NULL, NULL, NULL, 80, 'ON_SALE', 520, 205, 180);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (1, '파스텔 색소 4종', 3500, '물감처럼 섞어 쓰는 파스텔 색소', NULL, NULL, NULL, NULL, 35, 'ON_SALE', 190, 61, 128);

-- 2. 슬랑이 ----------------------------------------------------------------
INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (2, '쭈욱 딸기 슬랑이', 5500, '끝없이 늘어나는 스트레칭 슬라임', '쫀득', 3, 5, '딸기', 25, 'ON_SALE', 460, 174, 390);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (2, '말차 라떼 슬랑이', 5000, '부드럽게 감기는 크림 타입', '탱글', 2, 4, '말차', 30, 'ON_SALE', 275, 96, 210);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (2, '우주 반짝 슬랑이', 6500, '글리터가 촘촘히 박힌 갤럭시 슬라임', '쫀득', 2, 5, '블루베리', 12, 'ON_SALE', 610, 288, 540);

-- 3. 크런치 슬랑이 -----------------------------------------------------------
INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (3, '크런치 구름빵', 4500, '누를 때마다 바스락 소리가 나요', '크런치', 5, 2, '바닐라', 20, 'ON_SALE', 380, 149, 305);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (3, '콘프레이크 크런치', 5000, '와그작 소리가 가장 큰 인기 크런치', '크런치', 5, 1, '시리얼', 0, 'ON_SALE', 720, 331, 620);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (3, '팝콘 버터 크런치', 5500, '고소한 향과 함께 톡톡 터지는 식감', '크런치', 4, 2, '버터팝콘', 22, 'ON_SALE', 240, 77, 195);

-- 4. 말랑이 ----------------------------------------------------------------
INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (4, '복숭아 젤리 말랑이', 3000, '한 손에 쏙 들어오는 기본 말랑이', '말랑', 2, 4, '복숭아', 30, 'ON_SALE', 850, 402, 710);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (4, '버터 토끼 스퀴시', 6000, '천천히 돌아오는 저반발 촉감', '버터', 1, 3, '우유', 15, 'ON_SALE', 330, 118, 288);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (4, '말랑 푸딩 볼', 4000, '탱글탱글 흔들리는 푸딩 타입', '탱글', 2, 3, '카라멜', 18, 'ON_SALE', 205, 64, 152);

INSERT INTO product (category_id, product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status, sales_count, review_count, like_count)
VALUES (4, '솜사탕 구름 쿠션', 7500, '가장 큰 사이즈, 안아도 말랑', '폭신', 1, 2, '솜사탕', 10, 'ON_SALE', 145, 53, 430);
