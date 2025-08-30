// フロント（:8000）→ BFF（:8080）へのクロスオリジン通信なので、credentials: 'include' が必須。
// BFF 側は CORS 設定で origin と credentials を許可しておくこと。
const API_BASE = 'http://localhost:8080';

const $ = (id) => document.getElementById(id);
const outJson = (el, obj) => el.textContent = JSON.stringify(obj, null, 2);

// 直近で使った口座IDは localStorage に保存
const ACCOUNT_KEY = 'accountId';
const loadAccountId = () => {
  const v = localStorage.getItem(ACCOUNT_KEY) || '';
  $('accountId').value = v;
};
const saveAccountId = (v) => localStorage.setItem(ACCOUNT_KEY, v || '');

async function me() {
  const r = await fetch(`${API_BASE}/secure/me`, { credentials: 'include' });
  const box = $('userBox');
  if (r.ok) {
    const j = await r.json();
    box.textContent = `Hello, ${j.name || j.preferred_username || 'user'} (${j.email || '-'})`;
  } else {
    box.textContent = `/secure/me => ${r.status}`;
  }
}

async function createAccount() {
  const owner = $('owner').value.trim();
  if (!owner) return alert('名義を入力してください');

  const r = await fetch(`${API_BASE}/api/savings/accounts`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ owner })
  });
  const j = await r.json().catch(() => ({}));
  outJson($('accountOut'), j);
  if (r.ok && j.id) {
    $('accountId').value = j.id;
    saveAccountId(j.id);
  } else {
    alert(`口座作成に失敗しました (${r.status})`);
  }
}

async function getAccount() {
  const id = $('accountId').value.trim();
  if (!id) return alert('口座IDを入力してください');
  const r = await fetch(`${API_BASE}/api/savings/accounts/${id}`, {
    credentials: 'include'
  });
  const j = await r.json().catch(() => ({}));
  outJson($('accountOut'), j);
  if (!r.ok) alert(`取得に失敗しました (${r.status})`);
}

async function deposit() {
  const id = $('accountId').value.trim();
  const amount = Number($('depAmount').value);
  if (!id) return alert('口座IDを入力してください');
  if (!(amount > 0)) return alert('金額を入力してください');

  const r = await fetch(`${API_BASE}/api/savings/accounts/${id}/deposit`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ amount })
  });
  const j = await r.json().catch(() => ({}));
  outJson($('accountOut'), j);
  if (!r.ok) alert(`入金に失敗しました (${r.status})`);
}

async function withdraw() {
  const id = $('accountId').value.trim();
  const amount = Number($('wdAmount').value);
  if (!id) return alert('口座IDを入力してください');
  if (!(amount > 0)) return alert('金額を入力してください');

  const r = await fetch(`${API_BASE}/api/savings/accounts/${id}/withdraw`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ amount })
  });
  const j = await r.json().catch(() => ({}));
  outJson($('accountOut'), j);
  if (!r.ok) alert(`出金に失敗しました (${r.status})`);
}

async function transfer() {
  const fromAccountId = $('accountId').value.trim();
  const months = Number($('plan').value);
  const amount = Number($('tdAmount').value);
  if (!fromAccountId) return alert('口座IDが必要です（先に作成/取得してください）');
  if (!(amount > 0)) return alert('金額を入力してください');

  // backend の TransferRequest に合わせてプロパティ名を送る
  const payload = { fromAccountId, amount, months };

  const r = await fetch(`${API_BASE}/api/time-deposits/transfers`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const j = await r.json().catch(() => ({}));
  outJson($('tdOut'), j);
  if (!r.ok) alert(`定期預金の作成に失敗しました (${r.status})`);
}

function login() {
  // Quarkus OIDC の認可フロー開始。BFF が Keycloak にリダイレクト → 最終的に http://localhost:8000/secure/callback へ戻る
  location.href = `${API_BASE}/secure/login`;
}

// イベント割り当て
window.addEventListener('DOMContentLoaded', () => {
  loadAccountId();
  $('loginBtn').addEventListener('click', login);
  $('meBtn').addEventListener('click', me);
  $('createBtn').addEventListener('click', createAccount);
  $('getAccountBtn').addEventListener('click', getAccount);
  $('depositBtn').addEventListener('click', deposit);
  $('withdrawBtn').addEventListener('click', withdraw);
  $('transferBtn').addEventListener('click', transfer);
  // 起動時にセッション確認
  me().catch(() => {});
});
