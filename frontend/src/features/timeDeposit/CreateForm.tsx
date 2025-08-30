import { useState } from "react";
import { apiFetch } from "../../api/client";
import Card from "../../components/Card";

export default function CreateForm() {
  const [form, setForm] = useState({ owner: "", principal: 10000, annualRate: 0.015, termDays: 30, fromAccountId: "" });
  const [resp, setResp] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    const key = id.replace("td-", "") as keyof typeof form;
    setForm(prev => ({ ...prev, [key]: (key === "principal" || key === "termDays" || key === "annualRate") ? Number(value) : value } as any));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setResp(null);
    try {
      const data = await apiFetch("/deposits", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      setResp(data);
    } catch (e: any) {
      setResp(e?.payload ?? { error: e.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="定期預金作成（普通から引落し）">
      <form onSubmit={submit}>
        <div className="row">
          <div className="field">
            <label>名義人 (owner)</label>
            <input id="td-owner" value={form.owner} onChange={onChange} placeholder="Hanako" required />
          </div>
          <div className="field">
            <label>元本 (principal)</label>
            <input id="td-principal" type="number" min={1} step={1} value={form.principal} onChange={onChange} required />
          </div>
        </div>

        <div className="row">
          <div className="field">
            <label>年利 (annualRate)</label>
            <input id="td-annualRate" type="number" min={0} step={0.0001} value={form.annualRate} onChange={onChange} required />
          </div>
          <div className="field">
            <label>預入日数 (termDays)</label>
            <input id="td-termDays" type="number" min={1} step={1} value={form.termDays} onChange={onChange} required />
          </div>
        </div>

        <div className="field">
          <label>引落口座ID (fromAccountId)</label>
          <input id="td-fromAccountId" value={form.fromAccountId} onChange={onChange} placeholder="fc8f799b-..." required />
        </div>

        <button disabled={loading} type="submit">{loading ? "送信中..." : "定期預金を作成"}</button>
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
