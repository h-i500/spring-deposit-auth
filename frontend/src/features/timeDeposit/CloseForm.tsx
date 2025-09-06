import { useState } from "react";
import { apiFetch } from "../../api/client";
import Card from "../../components/Card";

export default function CloseForm() {
  const [depositId, setDepositId] = useState("");
  const [toAccountId, setTo] = useState("");
  const [at, setAt] = useState("");
  const [resp, setResp] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!depositId || !toAccountId) return;
    setLoading(true);
    setResp(null);
    try {
      const q = new URLSearchParams({ toAccountId, at: at || new Date().toISOString() });
      const data = await apiFetch(`/deposits/${encodeURIComponent(depositId)}/close?${q.toString()}`, { method: "POST" });
      setResp(data);
    } catch (e: any) {
      setResp(e?.payload ?? { error: e.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="定期預金 満期解約（普通へ自動振替）">
      <form onSubmit={submit}>
        <div className="row">
          <div className="field">
            <label>定期預金ID (depositId)</label>
            <input value={depositId} onChange={(e)=>setDepositId(e.target.value)} placeholder="dc8bee77-..." required />
          </div>
          <div className="field">
            <label>振替先 口座ID (toAccountId)</label>
            <input value={toAccountId} onChange={(e)=>setTo(e.target.value)} placeholder="fc8f799b-..." required />
          </div>
        </div>
        <div className="field">
          <label>解約日時 at (ISO8601)</label>
          <div style={{ display: "flex", gap: 8 }}>
            <input value={at} onChange={(e)=>setAt(e.target.value)} placeholder="2025-09-16T11:56:00.202Z" />
            <button type="button" onClick={()=>setAt(new Date().toISOString())}>現在時刻をセット</button>
          </div>
        </div>
        <button disabled={loading} type="submit">{loading ? "送信中..." : "満期解約を実行"}</button>
      </form>

      {resp && (
        <details open style={{ marginTop: 8 }}>
          <summary>レスポンス</summary>
          <pre>{JSON.stringify(resp, null, 2)}</pre>
        </details>
      )}
    </Card>
  );
}
