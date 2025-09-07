// frontend/src/features/savings/SavingsPanel.tsx
import { useEffect, useState } from "react";
import type { SavingsAccount } from "./api";
import {
  createAccount,
  deposit,
  withdraw,
  clearSavedAccountId,
  ACCOUNT_ID_KEY,
  getAccountById,
  getMyAccountIfExists,
  AuthRequiredError,
} from "./api";
import { useAuth } from "../auth/useAuth"; // ★ 追加

export default function SavingsPanel() {
  const { isAuthenticated, loading: authLoading, promptLogin } = useAuth(); // ★
  const [account, setAccount] = useState<SavingsAccount | null>(null);
  const [owner, setOwner] = useState<string>("Demo User");
  const [amount, setAmount] = useState<string>("1000");
  const [loading, setLoading] = useState<boolean>(false);
  const [err, setErr] = useState<string>("");

  // 初期表示：保存IDがあれば「取得」だけする（★作成しない★）
  useEffect(() => {
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const acc = await getMyAccountIfExists();
        setAccount(acc);
      } catch (e: any) {
        if (e instanceof AuthRequiredError) {
          // 未認証 → 何もしない（UIでログインを促す）
        } else {
          setErr(e?.message ?? String(e));
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const onCreateNew = async () => {
    if (!isAuthenticated) {
      // ★ 自動作成禁止：未認証ならログインだけ促す
      setErr("ログインが必要です。ログインすると作成できます。");
      promptLogin();
      return;
    }
    setLoading(true);
    setErr("");
    try {
      clearSavedAccountId();
      const acc = await createAccount(owner || "Demo User");
      try { localStorage.setItem(ACCOUNT_ID_KEY, acc.id); } catch {}
      setAccount(acc);
    } catch (e: any) {
      if (e instanceof AuthRequiredError) {
        setErr("ログインセッションが切れました。再度ログインしてください。");
        promptLogin();
      } else {
        setErr(e?.message ?? String(e));
      }
    } finally {
      setLoading(false);
    }
  };

  const onDeposit = async () => {
    if (!account) return;
    if (!isAuthenticated) { setErr("ログインが必要です。"); promptLogin(); return; }

    const amt = Number(amount);
    if (!Number.isFinite(amt) || amt <= 0) {
      setErr("金額を正しく入力してください。");
      return;
    }
    setLoading(true);
    setErr("");
    try {
      const acc = await deposit(account.id, amt);
      setAccount(acc);
    } catch (e: any) {
      if (e instanceof AuthRequiredError) {
        setErr("ログインセッションが切れました。再度ログインしてください。");
        promptLogin();
      } else {
        setErr(e?.message ?? String(e));
      }
    } finally {
      setLoading(false);
    }
  };

  const onWithdraw = async () => {
    if (!account) return;
    if (!isAuthenticated) { setErr("ログインが必要です。"); promptLogin(); return; }

    const amt = Number(amount);
    if (!Number.isFinite(amt) || amt <= 0) {
      setErr("金額を正しく入力してください。");
      return;
    }
    setLoading(true);
    setErr("");
    try {
      const acc = await withdraw(account.id, amt);
      setAccount(acc);
    } catch (e: any) {
      if (e instanceof AuthRequiredError) {
        setErr("ログインセッションが切れました。再度ログインしてください。");
        promptLogin();
      } else {
        setErr(e?.message ?? String(e));
      }
    } finally {
      setLoading(false);
    }
  };

  const onInquiry = async () => {
    if (!account) return;
    setLoading(true);
    setErr("");
    try {
      const fresh = await getAccountById(account.id);
      setAccount(fresh);
    } catch (e: any) {
      if (e instanceof AuthRequiredError) {
        setErr("ログインセッションが切れました。再度ログインしてください。");
        promptLogin();
      } else {
        setErr(e?.message ?? String(e));
      }
    } finally {
      setLoading(false);
    }
  };

  const disabledByAuth = authLoading || !isAuthenticated;

  return (
    <div style={{ padding: 16 }}>
      <h2>普通預金</h2>

      <div style={{ marginTop: 8 }}>
        <label>
          口座名義：
          <input
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            placeholder="Demo User"
            style={{ marginLeft: 8 }}
            disabled={disabledByAuth}
          />
        </label>
        <button style={{ marginLeft: 8 }} onClick={onCreateNew} disabled={loading || disabledByAuth}>
          新規口座を作成
        </button>
        {!isAuthenticated && !authLoading && (
          <button style={{ marginLeft: 8 }} onClick={() => promptLogin()}>
            ログイン
          </button>
        )}
      </div>

      {loading && <p style={{ marginTop: 8 }}>処理中...</p>}
      {err && (
        <p style={{ marginTop: 8, color: "crimson", whiteSpace: "pre-wrap" }}>
          エラー: {err}
        </p>
      )}

      <div style={{ marginTop: 16, padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
        <h3>現在の口座</h3>
        {account ? (
          <>
            <div>ID: <code>{account.id}</code></div>
            <div>名義: {account.owner}</div>
            <div style={{ fontSize: 18, marginTop: 4 }}>
              残高: <b>{account.balance.toLocaleString()}</b>
            </div>
          </>
        ) : (
          <div>口座がありません。ログイン後に「新規口座を作成」を押してください。</div>
        )}
      </div>

      <div style={{ marginTop: 16 }}>
        <label>
          金額：
          <input
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            style={{ marginLeft: 8 }}
            disabled={disabledByAuth || !account}
          />
        </label>
        <button style={{ marginLeft: 8 }} onClick={onDeposit} disabled={loading || disabledByAuth || !account}>
          入金
        </button>
        <button style={{ marginLeft: 8 }} onClick={onWithdraw} disabled={loading || disabledByAuth || !account}>
          出金
        </button>
        <button style={{ marginLeft: 8 }} onClick={onInquiry} disabled={loading || !account}>
          残高照会
        </button>
      </div>
    </div>
  );
}
