/* 말랑말랑 꾹꾹샵 - 백엔드 API 연동 */

const EMOJI = ['🍡', '🐰', '🍮', '🧁', '🐣', '🍑', '🫧', '🐻', '🍓', '🌸'];
const cart = new Map();   // productId -> { product, qty }
let me = null;            // 로그인한 고객 정보

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

/* ---------- 상품 목록 ---------- */

async function loadProducts() {
	const grid = document.getElementById('productGrid');

	try {
		const paged = await api('/api/products?offset=0&count=50');
		const list = paged.list || [];

		document.getElementById('productCount').textContent = `${paged.total}종`;

		if (list.length === 0) {
			grid.innerHTML = '<div class="empty">아직 등록된 말랑이가 없어요 🥲</div>';
			return;
		}

		grid.innerHTML = '';
		list.forEach((p, i) => grid.appendChild(productCard(p, i)));

	} catch (e) {
		grid.innerHTML = `<div class="empty">말랑이를 불러오지 못했어요<br><small>${e.message}</small></div>`;
	}
}

function productCard(p, index) {
	const card = document.createElement('div');
	card.className = 'card';

	const soldOut = (p.stockQuantity ?? 0) <= 0;

	// 꾹꾹 리포트 - 값이 있는 항목만 표시
	const chips = [];
	if (p.texture) chips.push(`<span class="chip">${p.texture}</span>`);
	if (p.scent) chips.push(`<span class="chip chip-mint">${p.scent}</span>`);
	if (p.status && p.status !== 'ON_SALE') chips.push(`<span class="chip chip-lemon">${p.status}</span>`);

	const meters = [
		['소리', p.soundLevel],
		['늘어남', p.stretchLevel]
	].filter(([, v]) => v != null)
	 .map(([label, v]) => `
		<div class="meter-row">
			<span class="meter-label">${label}</span>
			<span class="meter"><span class="meter-fill" style="width:${(v / 5) * 100}%"></span></span>
		</div>`)
	 .join('');

	card.innerHTML = `
		<div class="card-emoji">${EMOJI[index % EMOJI.length]}</div>
		<h3 class="card-name">${p.productName}</h3>
		<p class="card-desc">${p.description || '꾹— 눌러보세요'}</p>
		${chips.length ? `<div class="chips">${chips.join('')}</div>` : ''}
		${meters}
		<div class="card-foot">
			<div>
				<div class="price">${won(p.productPrice)}</div>
				<div class="stock ${soldOut ? 'out' : ''}">${soldOut ? '품절' : `남은 수량 ${p.stockQuantity}개`}</div>
			</div>
			<button class="btn btn-pink" ${soldOut ? 'disabled style="opacity:.4;box-shadow:none"' : ''}>담기</button>
		</div>`;

	if (!soldOut) {
		card.querySelector('button').addEventListener('click', () => addToCart(p));
	}
	return card;
}

/* ---------- 장바구니 ---------- */

function addToCart(product) {
	const entry = cart.get(product.id);

	if (entry) {
		if (entry.qty >= product.stockQuantity) {
			toast('재고보다 많이 담을 수 없어요', true);
			return;
		}
		entry.qty += 1;
	} else {
		cart.set(product.id, { product, qty: 1 });
	}

	renderCart();
	toast(`${product.productName} 담았어요 🧺`);
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

	if (cart.size === 0) {
		list.innerHTML = '<div class="empty small">아직 담은 말랑이가 없어요</div>';
		return;
	}

	list.innerHTML = '';
	cart.forEach(({ product, qty }) => {
		const row = document.createElement('div');
		row.className = 'cart-item';
		row.innerHTML = `
			<div class="cart-item-name">
				${product.productName}
				<div class="cart-item-sub">${won(product.productPrice * qty)}</div>
			</div>
			<div class="qty">
				<button data-minus>−</button>
				<span>${qty}</span>
				<button data-plus>+</button>
			</div>`;

		row.querySelector('[data-minus]').addEventListener('click', () => changeQty(product.id, -1));
		row.querySelector('[data-plus]').addEventListener('click', () => changeQty(product.id, 1));
		list.appendChild(row);
	});
}

/* ---------- 주문 ---------- */

async function placeOrder() {
	if (!me) {
		toast('로그인이 필요해요 🐰', true);
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

		toast(`주문 완료! ${order.orderNumber} 🎀`);
		cart.clear();
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
			<button class="btn btn-ghost" data-open="login">로그인</button>
			<button class="btn btn-pink" data-open="signup">회원가입</button>`;
	} else {
		box.innerHTML = `
			<span class="hello">${me.customerName || me.customerId}님</span>
			<span class="point-chip">🍯 ${won(me.customerPoint)}</span>`;
	}
	bindOpeners();
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
		toast(`반가워요, ${me.customerName || me.customerId}님 🐰`);
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
		toast('가입 완료! 이제 로그인해 주세요 🍡');
		closeModals();
		document.getElementById('loginId').value = payload.customerId;
		openModal('login');
	} catch (e) {
		toast(e.message, true);
	}
}

/* ---------- 열고 닫기 ---------- */

const openModal  = name => document.getElementById(`modal-${name}`).classList.add('open');
const closeModals = () => document.querySelectorAll('.modal').forEach(m => m.classList.remove('open'));
const closeDrawer = () => document.getElementById('cartDrawer').classList.remove('open');

function bindOpeners() {
	document.querySelectorAll('[data-open]').forEach(btn => {
		btn.onclick = () => openModal(btn.dataset.open);
	});
}

document.getElementById('cartFab').addEventListener('click', () => {
	document.getElementById('cartDrawer').classList.toggle('open');
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

/* ---------- 시작 ---------- */

bindOpeners();
renderCart();
loadProducts();
