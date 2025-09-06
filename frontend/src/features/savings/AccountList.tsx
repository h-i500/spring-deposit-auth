import { useEffect, useState } from "react";
import { apiFetch } from "../../api/client";

type Account = {
  id: string;
  owner?: string;
  balance?: number;
  status?: string;
  [k: string]: any;
};

export default function AccountList() {
  const [rows, setRows] = useState<Account[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    setErr(null);
    try {
      // 代表的に /savings/accounts を想定（既存APIに合わせてパスを調整）
      const data = await apiFetch("/savings/accounts");
      setRows(Array.isArray(data) ? data : (data.items || []));
    } catch (e: any) {
      setErr(e?.payload ? JSON.stringify(e.payload, null, 2) : e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <>
      <div style={{ display: "flex", gap: 8 }}>
        <button onClick={load} disabled={loading}>
          {loading ? "更新中..." : "一覧を更新"}
        </button>
      </div>

      {err && <pre>{err}</pre>}

      <table className="table">
        <thead>
          <tr>
            <th>Account ID</th>
            <th>Owner</th>
            <th>Balance</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td><code>{r.id}</code></td>
              <td>{r.owner ?? "-"}</td>
              <td>{typeof r.balance === "number" ? r.balance.toFixed(2) : "-"}</td>
              <td>{r.status ?? "-"}</td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr><td colSpan={4} className="hint">アカウントがありません。</td></tr>
          )}
        </tbody>
      </table>
      <p className="hint">※ エンドポイントが異なる場合は <code>AccountList.tsx</code> を調整してください。</p>
    </>
  );
}
