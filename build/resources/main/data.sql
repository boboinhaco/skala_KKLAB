-- 초기 상품 시드 데이터 (말랑이 컬렉션)
-- stock_quantity, status 는 NOT NULL 컬럼이므로 반드시 값을 넣어야 합니다.

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('복숭아 젤리 말랑이', 3000, '한 손에 쏙 들어오는 기본 말랑이', '말랑', 2, 4, '복숭아', 30, 'ON_SALE');

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('크런치 구름빵', 4500, '누를 때마다 바스락 소리가 나요', '크런치', 5, 2, '바닐라', 20, 'ON_SALE');

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('버터 토끼 스퀴시', 6000, '천천히 돌아오는 저반발 촉감', '버터', 1, 3, '우유', 15, 'ON_SALE');

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('쭈욱 딸기 슬라임', 5500, '끝없이 늘어나는 스트레칭 말랑이', '쫀득', 3, 5, '딸기', 25, 'ON_SALE');

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('말차 푸딩 볼', 4000, '탱글탱글 흔들리는 푸딩 타입', '탱글', 2, 3, '말차', 18, 'ON_SALE');

INSERT INTO product (product_name, product_price, description, texture, sound_level, stretch_level, scent, stock_quantity, status)
VALUES ('솜사탕 구름 쿠션', 7500, '가장 큰 사이즈, 안아도 말랑', '폭신', 1, 2, '솜사탕', 10, 'ON_SALE');
