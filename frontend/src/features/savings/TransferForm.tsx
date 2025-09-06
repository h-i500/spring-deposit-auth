import { useState } from "react";
import { apiFetch } from "../../api/client";

export default function TransferForm() {
  const [fromAccountId, setFrom] = useState("");
  const [toAccountId, setTo] = useState("");
  const [amount, setAmount] = useState<number>(0);
  const [resp, setResp] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!fromAccountId || !toAccountId || amount <= 0) return;

    setLoading(true);
    setResp(null);
    try {
      // 例: POST /savings/transfers { fromAccountId, toAccountId, amount }
      const data = await apiFetch("/savings/transfers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fromAccountId, toAccountId, amount })
      });
      setResp(data);
    } catch (e: any) {
      setResp(e?.payload ? e.payload : { error: e.message });
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <form onSubmit={submit}>
        <div className="row">
          <div className="field">
            <label>From Account ID</label>
            <input value={fromAccountId} onChange={e=>setFrom(e.target.value)} placeholder="fc8f799b-..." required />
          </div>
          <div className="field">
            <label>To Account ID</label>
            <input value={toAccountId} onChange={e=>setTo(e.target.value)} placeholder="fc8f799b-..." required />
          </div>
        </div>
        <div className="field">
          <label>金額 (amount)</label>
          <input type="number" min={1} step={1} value={amount || 0} onChange={e=>setAmount(Number(e.target.value))} required />
        </div>
        <button type="submit" disabled={loading}>{loading ? "送信中..." : "振替を実行"}</button>
      </form>

      {resp && (
        <details open style={{ marginTop: 8 }}>
          <summary>レスポンス</summary>
          <pre>{JSON.stringify(resp, null, 2)}</pre>
        </details>
      )}
      <p className="hint">※ 既存の Savings API の仕様に合わせてエンドポイントや JSON を調整してください。</p>
    </>
  );
}
