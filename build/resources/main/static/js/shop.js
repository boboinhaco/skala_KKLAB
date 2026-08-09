/* 말랑연구소 - 백엔드 API 연동 */

// categoryId 는 백엔드 product.category_id 와 1:1로 맞춰져 있음
const CATEGORIES = [
	{ id: null, name: '전체',         img: '/img/cat-all.svg' },
	{ id: 1,    name: '만들기 재료',   img: '/img/cat-material.svg' },
	{ id: 2,    name: '슬랑이',       img: '/img/cat-slime.svg' },
	{ id: 3,    name: '크런치 슬랑이', img: '/img/cat-crunch.svg' },
	{ id: 4,    name: '말랑이',       img: '/img/cat-squishy.svg' }
];

const SORTS = [
	{ key: 'likeCount',   name: '인기순' },
	{ key: 'salesCount',  name: '판매순' },
	{ key: 'reviewCount', name: '후기순' }
];

const KIT_LABEL = { 1: 'MATERIAL', 2: 'SLIME', 3: 'CRUNCH', 4: 'SQUISHY' };

// 표본 덩어리 색 (핑크 계열 + 카테고리별 포인트)
const BLOB_COLOR = {
	1: ['#e9f2cf', '#dcebf7', '#f7e6cf', '#ece0f7'],
	2: ['#ffd7ea', '#d9f0e6', '#dae4f7', '#f0dcf7'],
	3: ['#f7e3c2', '#efe6d2', '#f7d9c2', '#e6dcc4'],
	4: ['#ffd9dd', '#f7e6d2', '#e9dcf7', '#d9eeea']
};
const FACE = {
	1: ['✳', '◍', '❋', '✺'],
	2: ['◠‿◠', '•ᴗ•', '◕ᴗ◕', '˘ᴗ˘'],
	3: ['◕‿◕', '≧▽≦', '•▿•', '◔ᴗ◔'],
	4: ['◡‿◡', '•ᴥ•', '˘ᵕ˘', '´•ᴗ•`']
};

let allProducts = [];
let activeCategory = null;
let activeSort = 'likeCount';
const cardQty = new Map();   // productId -> 카드에서 고른 수량
let detailQty = 1;

const cart = new Map();      // productId -> { product, qty }
let me = null;               // 로그인한 고객 정보

/* ---------- 공통 ---------- */

// 응답 포맷: { result, code, message, body }
async function api(path, options = {}) {
	const res = await fetch(path, {
		headers: { 'Content-Type': 'application/json' },
		...options
	});
	const data = await res.json().catch(() => ({}));

	if (!res.ok || data.result === 'fail') {
		throw new Error(data.message || '요청을 처리하지 못했어요');
	}
	return data.body;
}

function toast(message, isBad = false) {
	const el = document.createElement('div');
	el.className = 'toast' + (isBad ? ' bad' : '');
	el.textContent = message;
	document.getElementById('toastArea').appendChild(el);
	setTimeout(() => el.remove(), 2600);
}

const won = n => (n ?? 0).toLocaleString('ko-KR') + 'P';
const num = n => (n ?? 0).toLocaleString('ko-KR');
const categoryName = id => (CATEGORIES.find(c => c.id === id) || {}).name || '기타';
const specimenNo = id => 'No.' + String(id ?? 0).padStart(3, '0');

function pick(table, product, fallback) {
	const pool = table[product.categoryId] || fallback;
	return pool[(product.id ?? 0) % pool.length];
}

// 카드 하단 색 점 - 카테고리 팔레트에서 3개
function dotsOf(p) {
	const pool = BLOB_COLOR[p.categoryId] || ['#ffd7ea'];
	const start = (p.id ?? 0) % pool.length;
	return [0, 1, 2].map(i => pool[(start + i) % pool.length]);
}

/* ---------- 카테고리 · 정렬 ---------- */

function renderTabs() {
	const nav = document.getElementById('categoryTabs');
	nav.innerHTML = '';

	CATEGORIES.forEach(cat => {
		const size = cat.id === null
			? allProducts.length
			: allProducts.filter(p => p.categoryId === cat.id).length;

		const btn = document.createElement('button');
		btn.className = 'cat' + (cat.id === activeCategory ? ' on' : '');
		btn.innerHTML = `
			<img class="cat-img" src="${cat.img}" alt="">
			<span class="cat-name">${cat.name}</span>
			<span class="cat-count">${String(size).padStart(2, '0')}</span>`;

		btn.addEventListener('click', () => {
			activeCategory = cat.id;
			renderTabs();
			renderProducts();
		});
		nav.appendChild(btn);
	});

	const sorts = document.getElementById('sortTabs');
	sorts.innerHTML = '';

	SORTS.forEach(s => {
		const btn = document.createElement('button');
		btn.className = 'sort' + (s.key === activeSort ? ' on' : '');
		btn.textContent = s.name;
		btn.addEventListener('click', () => {
			activeSort = s.key;
			renderTabs();
			renderProducts();
		});
		sorts.appendChild(btn);
	});
}

// 현재 카테고리 + 정렬이 적용된 목록
function visibleProducts() {
	const list = activeCategory === null
		? [...allProducts]
		: allProducts.filter(p => p.categoryId === activeCategory);

	return list.sort((a, b) => (b[activeSort] ?? 0) - (a[activeSort] ?? 0));
}

/* ---------- 상품 목록 ---------- */

async function loadProducts() {
	const grid = document.getElementById('productGrid');

	try {
		const paged = await api('/api/products?offset=0&count=100');
		allProducts = paged.list || [];
		renderTabs();
		renderProducts();
		renderLabStats();

	} catch (e) {
		grid.innerHTML = `<div class="empty">표본을 불러오지 못했어요<br><small>${e.message}</small></div>`;
	}
}

// 연구노트 섹션의 숫자
function renderLabStats() {
	const reviews = allProducts.reduce((sum, p) => sum + (p.reviewCount || 0), 0);
	document.getElementById('statTotal').textContent = allProducts.length;
	document.getElementById('statReviews').textContent = num(reviews);
	document.getElementById('hudStock').textContent = `${allProducts.length} SPEC`;
}

function renderProducts() {
	const grid = document.getElementById('productGrid');
	const list = visibleProducts();

	document.getElementById('sectionTitle').textContent =
		activeCategory === null ? '전체 상품' : categoryName(activeCategory);
	document.getElementById('productCount').textContent = `${String(list.length).padStart(2, '0')} ITEMS`;

	if (list.length === 0) {
		grid.innerHTML = '<div class="empty">이 카테고리에는 아직 표본이 없어요</div>';
		return;
	}

	grid.innerHTML = '';
	list.forEach(p => grid.appendChild(productItem(p)));
}

function blobHtml(p) {
	return `<span class="blob-shape" style="background:${pick(BLOB_COLOR, p, ['#ffd7ea'])}">${pick(FACE, p, ['◡‿◡'])}</span>`;
}

/* 상세용 표본 뷰 4컷 - 각도별로 색과 형태를 다르게 기록 */
const VIEW_SHAPE = [
	'46% 54% 60% 40% / 52% 44% 56% 48%',
	'62% 38% 44% 56% / 42% 58% 42% 58%',
	'40% 60% 52% 48% / 60% 40% 60% 40%',
	'55% 45% 38% 62% / 48% 62% 38% 52%'
];

function viewsOf(p) {
	const colors = BLOB_COLOR[p.categoryId] || ['#ffd7ea'];
	const faces = FACE[p.categoryId] || ['◡‿◡'];
	const start = (p.id ?? 0) % colors.length;

	return ['FRONT', 'SIDE', 'MACRO', 'SQUISHED'].map((tag, i) => ({
		tag,
		color: colors[(start + i) % colors.length],
		face: faces[(start + i) % faces.length],
		shape: VIEW_SHAPE[i]
	}));
}

const viewBlob = v =>
	`<span class="blob-shape" style="background:${v.color}; border-radius:${v.shape}">${v.face}</span>`;

function productItem(p) {
	const soldOut = (p.stockQuantity ?? 0) <= 0;
	const qty = cardQty.get(p.id) || 1;

	const item = document.createElement('div');
	item.className = 'item';
	item.innerHTML = `
		<div class="shot">
			<span class="shot-no mono">${specimenNo(p.id)}</span>
			<span class="shot-kit mono">${KIT_LABEL[p.categoryId] || 'KIT'}</span>
			${blobHtml(p)}
			${soldOut ? '<div class="shot-sold">SOLD OUT</div>' : ''}
		</div>

		<div class="item-row">
			<span class="item-name">${p.productName}</span>
			<span class="item-price">${won(p.productPrice)}</span>
		</div>

		<div class="dots">
			${dotsOf(p).map(c => `<span class="dot" style="background:${c}"></span>`).join('')}
		</div>

		<div class="item-buy">
			<span class="stepper">
				<button data-minus aria-label="수량 줄이기">−</button>
				<span data-qty>${qty}</span>
				<button data-plus aria-label="수량 늘리기">+</button>
			</span>
			<button class="add" data-add ${soldOut ? 'disabled' : ''}>담기</button>
		</div>`;

	item.querySelector('.shot').addEventListener('click', () => openDetail(p.id));

	const label = item.querySelector('[data-qty]');

	item.querySelector('[data-minus]').addEventListener('click', () => {
		const next = Math.max(1, (cardQty.get(p.id) || 1) - 1);
		cardQty.set(p.id, next);
		label.textContent = next;
	});

	item.querySelector('[data-plus]').addEventListener('click', () => {
		const next = (cardQty.get(p.id) || 1) + 1;
		if (next > p.stockQuantity) {
			toast('재고보다 많이 담을 수 없어요', true);
			return;
		}
		cardQty.set(p.id, next);
		label.textContent = next;
	});

	if (!soldOut) {
		item.querySelector('[data-add]').addEventListener('click', () => {
			addToCart(p, cardQty.get(p.id) || 1);
		});
	}
	return item;
}

/* ---------- 상품 상세 팝업 ---------- */

const meterHtml = v =>
	`<span class="meter"><span class="meter-fill" style="width:${(v / 5) * 100}%"></span></span><span class="mono">${v}/5</span>`;

function openDetail(productId) {
	const p = allProducts.find(x => x.id === productId);
	if (!p) return;

	detailQty = 1;
	const soldOut = (p.stockQuantity ?? 0) <= 0;
	const views = viewsOf(p);

	const spec = [['분류', categoryName(p.categoryId)]];
	if (p.texture) spec.push(['촉감', p.texture]);
	if (p.scent) spec.push(['향', p.scent]);
	if (p.soundLevel != null) spec.push(['소리', meterHtml(p.soundLevel)]);
	if (p.stretchLevel != null) spec.push(['늘어남', meterHtml(p.stretchLevel)]);
	spec.push(['재고', soldOut ? '재고 없음' : `${p.stockQuantity}개`]);
	spec.push(['기록', `찜 ${num(p.likeCount)} · 판매 ${num(p.salesCount)} · 후기 ${num(p.reviewCount)}`]);

	document.getElementById('detailBody').innerHTML = `
		<div class="detail-grid">
			<div class="detail-visual">
				<span class="shot-no mono">${specimenNo(p.id)}</span>
				<span class="shot-kit mono">${KIT_LABEL[p.categoryId] || 'KIT'}</span>

				<div class="gallery-main" id="galleryMain">
					${viewBlob(views[0])}
					<span class="view-tag">${views[0].tag}</span>
					<span class="view-index">01 / 0${views.length}</span>
				</div>

				<div class="thumbs">
					${views.map((v, i) => `
						<button class="thumb${i === 0 ? ' on' : ''}" data-view="${i}" aria-label="${v.tag} 컷 보기">
							${viewBlob(v)}
						</button>`).join('')}
				</div>
			</div>

			<div class="detail-info">
				<span class="mono pink">SPECIMEN REPORT</span>
				<h2 class="detail-name">${p.productName}</h2>
				<p class="detail-desc">${p.description || '꾹— 눌러보세요'}</p>
				<div class="detail-price">${won(p.productPrice)}</div>

				<div class="report">
					<div class="report-head">
						<span class="mono">MEASUREMENT</span>
						<span class="mono">${specimenNo(p.id)}</span>
					</div>
					${spec.map(([k, v]) => `
						<div class="spec-row">
							<span class="spec-key">${k}</span>
							<span class="spec-val">${v}</span>
						</div>`).join('')}
				</div>

				<div class="detail-buy">
					<span class="stepper">
						<button data-minus aria-label="수량 줄이기">−</button>
						<span id="detailQty">1</span>
						<button data-plus aria-label="수량 늘리기">+</button>
					</span>
					<button class="btn btn-pink" data-detail-add ${soldOut ? 'disabled' : ''}>
						${soldOut ? '재고가 없어요' : '장바구니에 담기'}
					</button>
				</div>
			</div>
		</div>`;

	const body = document.getElementById('detailBody');
	const label = body.querySelector('#detailQty');

	// 썸네일을 누르면 큰 컷 교체
	const main = body.querySelector('#galleryMain');
	body.querySelectorAll('.thumb').forEach(btn => {
		btn.addEventListener('click', () => {
			const i = Number(btn.dataset.view);
			const v = views[i];

			main.innerHTML = `
				${viewBlob(v)}
				<span class="view-tag">${v.tag}</span>
				<span class="view-index">0${i + 1} / 0${views.length}</span>`;

			body.querySelectorAll('.thumb').forEach(b => b.classList.toggle('on', b === btn));
		});
	});

	body.querySelector('[data-minus]').addEventListener('click', () => {
		detailQty = Math.max(1, detailQty - 1);
		label.textContent = detailQty;
	});

	body.querySelector('[data-plus]').addEventListener('click', () => {
		if (detailQty >= p.stockQuantity) {
			toast('재고보다 많이 담을 수 없어요', true);
			return;
		}
		detailQty += 1;
		label.textContent = detailQty;
	});

	if (!soldOut) {
		body.querySelector('[data-detail-add]').addEventListener('click', () => {
			addToCart(p, detailQty);
			closeModals();
		});
	}

	openModal('detail');
	document.getElementById('detailCard').scrollTop = 0;
}

/* ---------- 장바구니 ---------- */

function addToCart(product, qty) {
	const entry = cart.get(product.id);
	const next = (entry ? entry.qty : 0) + qty;

	if (next > product.stockQuantity) {
		toast('재고보다 많이 담을 수 없어요', true);
		return;
	}

	if (entry) {
		entry.qty = next;
	} else {
		cart.set(product.id, { product, qty });
	}

	renderCart();
	toast(`${product.productName} ${qty}개 담았어요`);
}

function changeQty(productId, delta) {
	const entry = cart.get(productId);
	if (!entry) return;

	const next = entry.qty + delta;

	if (next <= 0) {
		cart.delete(productId);
	} else if (next > entry.product.stockQuantity) {
		toast('재고보다 많이 담을 수 없어요', true);
		return;
	} else {
		entry.qty = next;
	}
	renderCart();
}

function cartTotal() {
	let sum = 0;
	cart.forEach(({ product, qty }) => sum += product.productPrice * qty);
	return sum;
}

function renderCart() {
	const list = document.getElementById('cartList');
	let totalQty = 0;
	cart.forEach(({ qty }) => totalQty += qty);

	document.getElementById('cartBadge').textContent = totalQty;
	document.getElementById('cartTotal').textContent = won(cartTotal());
	document.getElementById('cartRailTotal').textContent = won(cartTotal());

	if (cart.size === 0) {
		list.innerHTML = '<div class="empty small">아직 담은 표본이 없어요</div>';
		return;
	}

	list.innerHTML = '';
	cart.forEach(({ product, qty }) => {
		const row = document.createElement('div');
		row.className = 'cart-item';
		row.innerHTML = `
			<div class="cart-item-name">
				${product.productName}
				<div class="cart-item-sub mono">${won(product.productPrice * qty)}</div>
			</div>
			<span class="stepper">
				<button data-minus>−</button>
				<span>${qty}</span>
				<button data-plus>+</button>
			</span>`;

		row.querySelector('[data-minus]').addEventListener('click', () => changeQty(product.id, -1));
		row.querySelector('[data-plus]').addEventListener('click', () => changeQty(product.id, 1));
		list.appendChild(row);
	});
}

/* ---------- 주문 ---------- */

async function placeOrder() {
	if (!me) {
		toast('로그인이 필요해요', true);
		openModal('login');
		return;
	}
	if (cart.size === 0) {
		toast('장바구니가 비어 있어요', true);
		return;
	}

	const items = [];
	cart.forEach(({ product, qty }) => items.push({ productId: product.id, quantity: qty }));

	try {
		const order = await api('/api/customers/order', {
			method: 'POST',
			body: JSON.stringify({
				items,
				receiverName: document.getElementById('receiverName').value || me.customerName || me.customerId,
				address1: document.getElementById('address1').value || '주소 미입력'
			})
		});

		toast(`주문 완료 · ${order.orderNumber}`);
		cart.clear();
		cardQty.clear();
		renderCart();
		closeDrawer();
		await Promise.all([loadProducts(), refreshMe()]);

	} catch (e) {
		toast(e.message, true);
	}
}

/* ---------- 계정 ---------- */

function renderAccount() {
	const box = document.getElementById('account');

	if (!me) {
		box.innerHTML = `
			<button class="link-btn" data-open="login">로그인</button>
			<button class="link-btn" data-open="signup">회원가입</button>`;
	} else {
		box.innerHTML = `
			<span class="hello">${me.customerName || me.customerId}</span>
			<span class="point-chip">${won(me.customerPoint)}</span>`;
	}

	// 따라다니는 메뉴의 포인트 칸
	document.getElementById('railPointVal').textContent = me ? won(me.customerPoint) : '로그인';
	bindOpeners();
}

/* ---------- 주문내역 · 주문취소 ---------- */

// "2026-08-08T14:30:22.51" -> "2026.08.08 14:30"
function fmtDate(iso) {
	if (!iso) return '';
	const [d, t] = iso.split('T');
	return `${d.replace(/-/g, '.')} ${(t || '').slice(0, 5)}`;
}

async function openOrders() {
	if (!me) {
		toast('로그인이 필요해요', true);
		openModal('login');
		return;
	}

	openModal('orders');
	await loadOrders();
}

async function loadOrders() {
	const body = document.getElementById('ordersBody');
	body.innerHTML = '<div class="empty small">불러오는 중…</div>';

	try {
		const orders = await api('/api/customers/orders');
		document.getElementById('railOrderVal').textContent = `${orders.length}건`;

		if (orders.length === 0) {
			body.innerHTML = '<div class="empty small">아직 주문한 표본이 없어요</div>';
			return;
		}

		body.innerHTML = orders.map(renderOrder).join('');

		// 주문 전체 취소
		body.querySelectorAll('[data-cancel-order]').forEach(btn => {
			btn.addEventListener('click', () => cancelOrder(Number(btn.dataset.cancelOrder)));
		});

		// 상품 하나만 취소
		body.querySelectorAll('[data-cancel-item]').forEach(btn => {
			btn.addEventListener('click', () => cancelOrder(
				Number(btn.dataset.orderId),
				Number(btn.dataset.cancelItem)
			));
		});

	} catch (e) {
		body.innerHTML = `<div class="empty small">${e.message}</div>`;
	}
}

function renderOrder(o) {
	const canceled = o.status === 'CANCELED';

	return `
		<article class="order">
			<header class="order-head">
				<div>
					<span class="order-no mono">${o.orderNumber}</span>
					<div class="order-meta">${fmtDate(o.orderedAt)} · ${o.receiverName || '-'}</div>
				</div>
				<span class="tag ${canceled ? 'tag-off' : 'tag-on'}">${o.status}</span>
			</header>

			${o.items.map(i => {
				const off = i.itemStatus === 'CANCELED';
				return `
					<div class="order-row ${off ? 'off' : ''}">
						<span class="order-name">
							${i.productName}
							<div class="order-meta">${won(i.unitPrice)} × ${i.quantity}</div>
						</span>
						<span class="order-price">${won(i.subtotal)}</span>
						${off
							? '<span class="tag tag-off">CANCELED</span>'
							: `<button class="mini-btn" data-cancel-item="${i.productId}" data-order-id="${o.orderId}">취소</button>`}
					</div>`;
			}).join('')}

			<footer class="order-foot">
				<span><span class="mono">TOTAL</span> <b>${won(o.totalAmount)}</b></span>
				${canceled
					? '<span class="mono order-done">전체 취소됨</span>'
					: `<button class="mini-btn strong" data-cancel-order="${o.orderId}">주문 전체 취소</button>`}
			</footer>
		</article>`;
}

// productId 를 주면 그 상품만, 없으면 주문 전체 취소
async function cancelOrder(orderId, productId) {
	const label = productId ? '이 상품을 취소할까요?' : '주문 전체를 취소할까요?';
	if (!window.confirm(`${label}\n취소하면 포인트가 환급되고 재고도 복구됩니다.`)) return;

	try {
		const payload = productId ? { orderId, productId } : { orderId };
		await api('/api/customers/cancel', { method: 'POST', body: JSON.stringify(payload) });

		toast('취소 완료 · 포인트가 환급되었어요');
		await Promise.all([loadOrders(), loadProducts(), refreshMe()]);

	} catch (e) {
		toast(e.message, true);
	}
}

// 주문 후 포인트 갱신
async function refreshMe() {
	if (!me) return;
	try {
		const detail = await api(`/api/customers/${me.customerId}`);
		me.customerPoint = detail.customerPoint;
		renderAccount();
	} catch { /* 조회 실패는 무시 */ }
}

async function login() {
	try {
		me = await api('/api/customers/login', {
			method: 'POST',
			body: JSON.stringify({
				customerId: document.getElementById('loginId').value.trim(),
				customerPassword: document.getElementById('loginPw').value
			})
		});
		renderAccount();
		closeModals();
		toast(`반가워요, ${me.customerName || me.customerId} 연구원`);
	} catch (e) {
		toast(e.message, true);
	}
}

async function signup() {
	const payload = {
		customerId: document.getElementById('signupId').value.trim(),
		customerPassword: document.getElementById('signupPw').value,
		customerName: document.getElementById('signupName').value.trim(),
		email: document.getElementById('signupEmail').value.trim()
	};
	if (!payload.customerName) delete payload.customerName;
	if (!payload.email) delete payload.email;

	try {
		await api('/api/customers', { method: 'POST', body: JSON.stringify(payload) });
		toast('등록 완료! 이제 로그인해 주세요');
		closeModals();
		document.getElementById('loginId').value = payload.customerId;
		openModal('login');
	} catch (e) {
		toast(e.message, true);
	}
}

/* ---------- 열고 닫기 ---------- */

function openModal(name) {
	document.getElementById(`modal-${name}`).classList.add('open');
	document.body.classList.add('locked');
}
function closeModals() {
	document.querySelectorAll('.modal').forEach(m => m.classList.remove('open'));
	document.body.classList.remove('locked');
}
const closeDrawer = () => document.getElementById('cartDrawer').classList.remove('open');

function bindOpeners() {
	document.querySelectorAll('[data-open]').forEach(btn => {
		btn.onclick = () => openModal(btn.dataset.open);
	});
}

const toggleDrawer = () => document.getElementById('cartDrawer').classList.toggle('open');
document.getElementById('cartFab').addEventListener('click', toggleDrawer);
document.getElementById('railOrders').addEventListener('click', openOrders);

// 포인트 칸 - 로그인 전이면 로그인, 후에는 최신 잔액을 다시 불러옴
document.getElementById('railPoint').addEventListener('click', async () => {
	if (!me) {
		openModal('login');
		return;
	}
	await refreshMe();
	toast(`보유 포인트 ${won(me.customerPoint)}`);
});

// 주문방법 STEP 03 에서 바로 장바구니 열기
document.getElementById('openCartFromHowto').addEventListener('click', () => {
	document.getElementById('cartDrawer').classList.add('open');
});
document.querySelector('[data-close-drawer]').addEventListener('click', closeDrawer);
document.querySelectorAll('[data-close-modal]').forEach(b => b.addEventListener('click', closeModals));
document.querySelectorAll('.modal').forEach(m => {
	m.addEventListener('click', e => { if (e.target === m) closeModals(); });
});
document.addEventListener('keydown', e => {
	if (e.key === 'Escape') { closeModals(); closeDrawer(); }
});

document.getElementById('loginBtn').addEventListener('click', login);
document.getElementById('signupBtn').addEventListener('click', signup);
document.getElementById('orderBtn').addEventListener('click', placeOrder);

/* ---------- 히어로 영상 ---------- */

// /img/hero.mp4 를 넣어두면 SVG 루프 대신 그 영상을 재생한다.
async function useHeroVideoIfPresent() {
	try {
		const res = await fetch('/img/hero.mp4', { method: 'HEAD' });
		if (!res.ok) return;

		const video = document.createElement('video');
		Object.assign(video, { src: '/img/hero.mp4', autoplay: true, loop: true, muted: true, playsInline: true });
		document.querySelector('.hero-loop').replaceWith(video);
	} catch { /* 파일이 없으면 SVG 루프 유지 */ }
}

/* ---------- 시작 ---------- */

useHeroVideoIfPresent();
renderAccount();
renderCart();
loadProducts();
