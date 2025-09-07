// frontend/src/features/savings/api.ts

// サービスの型（必要最小限）
export type SavingsAccount = {
  id: string;
  owner: string;
  balance: number;
};

// ローカル保存するキー
export const ACCOUNT_ID_KEY = "accountId";

// // 共通Fetchヘルパー
// async function api<T>(path: string, init?: RequestInit): Promise<T> {
//   const res = await fetch(path, {
//     credentials: "include",
//     ...init,
//     headers: {
//       ...(init?.headers || {}),
//       ...(init?.body ? { "content-type": "application/json" } : {}),
//     },
//   });

//   if (!res.ok) {
//     const text = await res.text().catch(() => "");
//     throw new Error(
//       `HTTP ${res.status} ${res.statusText}${text ? ` - ${text}` : ""}`
//     );
//   }

//   // JSON 以外のレスポンスは基本使わない想定
//   const ct = res.headers.get("content-type") || "";
//   return (ct.includes("application/json")
//     ? await res.json()
//     : (undefined as unknown)) as T;
// }

/**
 * 口座作成: POST /api/savings/accounts
 * @param owner 口座名義（未指定なら Demo User）
 */
export async function createAccount(
  owner: string = "Demo User"
): Promise<SavingsAccount> {
  return api<SavingsAccount>("/api/savings/accounts", {
    method: "POST",
    body: JSON.stringify({ owner }),
  });
}


// 自動作成されてしまうので、削除
// /**
//  * 既存IDをlocalStorageから取得。なければ新規作成して保存し、IDを返す。
//  */
// export async function ensureAccount(owner = "Demo User"): Promise<string> {
//   try {
//     const saved = localStorage.getItem(ACCOUNT_ID_KEY);
//     if (saved) return saved;
//   } catch {
//     // SSR等でlocalStorageなしの場合はスキップ
//   }

//   const created = await createAccount(owner);
//   try {
//     localStorage.setItem(ACCOUNT_ID_KEY, created.id);
//   } catch {
//     /* ignore */
//   }
//   return created.id;
// }

// 代わりに「保存 ID があればそれを返すだけ」
export function getSavedAccountId(): string | null {
  try { return localStorage.getItem(ACCOUNT_ID_KEY); } catch { return null; }
}
// 保存 ID があれば取得。無ければ null を返す（作らない）
export async function getMyAccountIfExists(): Promise<SavingsAccount | null> {
  const id = getSavedAccountId();
  if (!id) return null;
  return getAccountById(id);
}



/**
 * 指定IDの口座詳細取得: GET /api/savings/accounts/{id}
 */
export async function getAccountById(
  accountId: string
): Promise<SavingsAccount> {
  return api<SavingsAccount>(
    `/api/savings/accounts/${encodeURIComponent(accountId)}`
  );
}

// /**
//  * 自分用の口座を必ず確保してから詳細取得
//  */
// export async function getOrCreateMyAccount(
//   owner = "Demo User"
// ): Promise<SavingsAccount> {
//   const id = await ensureAccount(owner);
//   return getAccountById(id);
// }

/**
 * 入金: POST /api/savings/accounts/{id}/deposit  { amount }
 */
export async function deposit(
  accountId: string,
  amount: number
): Promise<SavingsAccount> {
  return api<SavingsAccount>(
    `/api/savings/accounts/${encodeURIComponent(accountId)}/deposit`,
    {
      method: "POST",
      body: JSON.stringify({ amount }),
    }
  );
}

/**
 * 出金: POST /api/savings/accounts/{id}/withdraw  { amount }
 */
export async function withdraw(
  accountId: string,
  amount: number
): Promise<SavingsAccount> {
  return api<SavingsAccount>(
    `/api/savings/accounts/${encodeURIComponent(accountId)}/withdraw`,
    {
      method: "POST",
      body: JSON.stringify({ amount }),
    }
  );
}

/**
 * 保存済みの口座IDを消す（リセットしたい時に使用）
 */
export function clearSavedAccountId(): void {
  try {
    localStorage.removeItem(ACCOUNT_ID_KEY);
  } catch {
    /* ignore */
  }
}


// 認証が必要な場合に投げる例外
export class AuthRequiredError extends Error {
  constructor(message = "認証が必要です") { super(message); this.name = "AuthRequiredError"; }
}

// 認証エラーを特別扱いするFetchヘルパー
async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    credentials: "include",
    redirect: "manual", // ★ 302 を勝手に追わない（BFFの /secure/login に行かない）
    ...init,
    headers: {
      ...(init?.headers || {}),
      ...(init?.body ? { "content-type": "application/json" } : {}),
    },
  });

  // 401 / 302 は未認証扱いにして、呼び出し側でログイン誘導だけする
  if (res.status === 401 || res.status === 302) {
    throw new AuthRequiredError();
  }

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText}${text ? ` - ${text}` : ""}`);
  }

  const ct = res.headers.get("content-type") || "";
  return (ct.includes("application/json")
    ? await res.json()
    : (undefined as unknown)) as T;
}