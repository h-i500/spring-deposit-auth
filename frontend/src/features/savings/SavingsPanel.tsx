// frontend/src/features/savings/SavingsPanel.tsx
import { useEffect, useMemo, useState } from "react";
import type { SavingsAccount } from "./api";
import {
  createAccount,
  deposit,
  withdraw,
  getAccountById,
  ACCOUNT_ID_KEY,
} from "./api";

const RECENTS_KEY = "recentAccountIds"; // 履歴保存用

function loadRecents(): string[] {
  try {
    const raw = localStorage.getItem(RECENTS_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr.slice(0, 10) : [];
  } catch { return []; }
}

function saveRecents(accountId: string) {
  try {
    const cur = loadRecents().filter((x) => x !== accountId);
    localStorage.setItem(RECENTS_KEY, JSON.stringify([accountId, ...cur]));
  } catch { /* ignore */ }
}

export default function SavingsPanel() {
  const [accountIdInput, setAccountIdInput] = useState<string>("");
  const [account, setAccount] = useState<SavingsAccount | null>(null);
  const [owner, setOwner] = useState<string>("Demo User");
  const [amount, setAmount] = useState<string>("1000");
  const [loading, setLoading] = useState<boolean>(false);
  const [err, setErr] = useState<string>("");

  const recents = useMemo(loadRecents, [account?.id]);

  // 初期表示: 直近IDがあれば GET だけする（自動作成しない）
  useEffect(() => {
    (async () => {
      try {
        const saved = localStorage.getItem(ACCOUNT_ID_KEY);
        if (!saved) return;
        setAccountIdInput(saved);
        setLoading(true);
        const acc = await getAccountById(saved);
        setAccount(acc);
      } catch (e: any) {
        setErr(e?.message ?? String(e));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const setActiveAccount = (acc: SavingsAccount) => {
    setAccount(acc);
    setAccountIdInput(acc.id);
    try { localStorage.setItem(ACCOUNT_ID_KEY, acc.id); } catch { /* ignore */ }
    saveRecents(acc.id);
  };

  const onLoadById = async () => {
    if (!accountIdInput.trim()) return;
    setLoading(true);
    setErr("");
    try {
      const acc = await getAccountById(accountIdInput.trim());
      setActiveAccount(acc);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  const onCreateNew = async () => {
    setLoading(true);
    setErr("");
    try {
      const acc = await createAccount(owner || "Demo User");
      setActiveAccount(acc);
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
      setActiveAccount(acc);
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
      setActiveAccount(acc);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  const onClearActive = () => {
    setAccount(null);
    setAccountIdInput("");
    try { localStorage.removeItem(ACCOUNT_ID_KEY); } catch { /* ignore */ }
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>普通預金</h2>

      {/* アクティブ口座の指定（IDで指定） */}
      <div style={{ marginTop: 8, display: "grid", gap: 8 }}>
        <div>
          <label>
            Account ID：
            <input
              value={accountIdInput}
              onChange={(e) => setAccountIdInput(e.target.value)}
              placeholder="xxxxxxxx-xxxx-...."
              style={{ marginLeft: 8, width: 320 }}
            />
          </label>
          <button style={{ marginLeft: 8 }} onClick={onLoadById} disabled={loading || !accountIdInput.trim()}>
            このIDで読込
          </button>
          <button style={{ marginLeft: 8 }} onClick={onClearActive} disabled={loading && !account}>
            クリア
          </button>
        </div>

        {/* 最近使ったIDから選択 */}
        {recents.length > 0 && (
          <div>
            <label>
              最近使ったID：
              <select
                value=""
                onChange={(e) => {
                  const v = e.target.value;
                  if (!v) return;
                  setAccountIdInput(v);
                }}
                style={{ marginLeft: 8, minWidth: 340 }}
              >
                <option value="">選択してください</option>
                {recents.map((id) => (
                  <option key={id} value={id}>{id}</option>
                ))}
              </select>
            </label>
          </div>
        )}
      </div>

      {/* 新規作成（明示操作のみ） */}
      <div style={{ marginTop: 12 }}>
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
          <div>アクティブな口座がありません。IDを指定して「このIDで読込」または「新規口座を作成」を押してください。</div>
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
        <button style={{ marginLeft: 8 }} onClick={onDeposit} disabled={loading || !account}>
          入金
        </button>
        <button style={{ marginLeft: 8 }} onClick={onWithdraw} disabled={loading || !account}>
          出金
        </button>
      </div>
    </div>
  );
}
