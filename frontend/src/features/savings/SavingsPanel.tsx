// frontend/src/features/savings/SavingsPanel.tsx
import { useEffect, useState } from "react";
import type { SavingsAccount } from "./api";
import {
  getOrCreateMyAccount,
  createAccount,
  deposit,
  withdraw,
  clearSavedAccountId,
  ACCOUNT_ID_KEY,
  getAccountById,
} from "./api";

export default function SavingsPanel() {
  const [account, setAccount] = useState<SavingsAccount | null>(null);
  const [owner, setOwner] = useState<string>("Demo User");
  const [amount, setAmount] = useState<string>("1000");
  const [loading, setLoading] = useState<boolean>(false);
  const [err, setErr] = useState<string>("");

  // 初期表示：既存IDがあれば取得、無ければ作成して取得
  useEffect(() => {
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const acc = await getOrCreateMyAccount(owner);
        setAccount(acc);
      } catch (e: any) {
        setErr(e?.message ?? String(e));
      } finally {
        setLoading(false);
      }
    })();
    // owner は初期作成時にのみ使いたいので、依存配列に含めない
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onCreateNew = async () => {
    setLoading(true);
    setErr("");
    try {
      // 保存済みIDをクリアして新規作成
      clearSavedAccountId();
      const acc = await createAccount(owner || "Demo User");
      try {
        localStorage.setItem(ACCOUNT_ID_KEY, acc.id);
      } catch {
        /* ignore */
      }
      setAccount(acc);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  const onDeposit = async () => {
    if (!account) return;
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
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  const onWithdraw = async () => {
    if (!account) return;
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
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  // 追加: 残高照会（最新の口座情報をサーバから再取得）
  const onInquiry = async () => {
    if (!account) return;
    setLoading(true);
    setErr("");
    try {
      const fresh = await getAccountById(account.id);
      setAccount(fresh);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

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
          />
        </label>
        <button style={{ marginLeft: 8 }} onClick={onCreateNew} disabled={loading}>
          新規口座を作成
        </button>
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
          <div>口座がありません。上の「新規口座を作成」を押すか、再読み込みしてください。</div>
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
          />
        </label>
        <button
          style={{ marginLeft: 8 }}
          onClick={onDeposit}
          disabled={loading || !account}
        >
          入金
        </button>
        <button
          style={{ marginLeft: 8 }}
          onClick={onWithdraw}
          disabled={loading || !account}
        >
          出金
        </button>
        <button
          style={{ marginLeft: 8 }}
          onClick={onInquiry}
          disabled={loading || !account}
          title="最新のサーバ値で残高を更新"
        >
          残高照会
        </button>
      </div>
    </div>
  );
}
