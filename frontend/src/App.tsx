import React, { useEffect, useState } from "react";
import { api } from "./lib/api";

type Me = { principal?: string; preferred_username?: string; email?: string };
type Account = { id: string; name: string; currency: string; balance: number };

export default function App() {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [products, setProducts] = useState<any[] | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const loadMe = () =>
    fetch("/secure/me", { credentials: "include" })
      .then(async (res) => (res.ok ? setMe(await res.json()) : setMe(null)))
      .catch(() => setMe(null))
      .finally(() => setLoading(false));

  useEffect(() => {
    loadMe();

    const recheck = () => document.visibilityState === "visible" && loadMe();
    document.addEventListener("visibilitychange", recheck as any);
    return () => {
      document.removeEventListener("visibilitychange", recheck as any);
    };
  }, []); // ← 依存配列は空（ビルドエラー回避・実装意図に合致）

  const login = () => (window.location.href = "/secure/login");
  const logout = () => (window.location.href = "/secure/logout");

  const loadAccounts = async () => {
    setErr(null);
    try {
      const data = await api<Account[]>("/api/savings/accounts");
      setAccounts(data);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const loadProducts = async () => {
    setErr(null);
    try {
      const data = await api<any[]>("/api/time-deposits/products");
      setProducts(data);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div style={{ fontFamily: "system-ui, sans-serif", padding: 24 }}>
      <h1>Deposit App</h1>

      {loading ? (
        <p>状態確認中...</p>
      ) : me ? (
        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
          <span>
            こんにちは、<strong>{me.preferred_username ?? me.principal}</strong> さん
          </span>
          <button onClick={logout}>ログアウト</button>
        </div>
      ) : (
        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
          <span>未ログインです。</span>
          <button onClick={login}>ログイン</button>
        </div>
      )}

      <hr style={{ margin: "16px 0" }} />

      <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
        <button onClick={loadAccounts} disabled={!me}>普通預金アカウント取得</button>
        <button onClick={loadProducts} disabled={!me}>定期預金商品取得</button>
      </div>

      {err && <p style={{ color: "crimson" }}>ERROR: {err}</p>}

      {accounts && (
        <div style={{ marginTop: 16 }}>
          <h3>普通預金</h3>
          <ul>
            {accounts.map((a) => (
              <li key={a.id}>
                {a.name} : {a.balance} {a.currency}
              </li>
            ))}
          </ul>
        </div>
      )}

      {products && (
        <div style={{ marginTop: 16 }}>
          <h3>定期預金商品</h3>
          <pre style={{ background: "#f6f8fa", padding: 12, borderRadius: 8 }}>
            {JSON.stringify(products, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
