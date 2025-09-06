import { useState } from "react";
import { fetchSavingsByOwner, fetchTimeDepositsByOwner } from "../../api/debug";
import type { SavingsAccountDto, TimeDepositDto } from "../../types/account";
import Card from "../../components/Card";

export default function DebugPanel() {
  const [key, setKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [savings, setSavings] = useState<SavingsAccountDto[] | null>(null);
  const [tds, setTds] = useState<TimeDepositDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onSearch = async () => {
    setLoading(true); setError(null);
    try {
      const [s, t] = await Promise.all([
        fetchSavingsByOwner(key),
        fetchTimeDepositsByOwner(key),
      ]);
      setSavings(s); setTds(t);
    } catch (e: any) {
      setError(e?.message ?? "検索に失敗しました");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section style={{ border: "1px dashed #bbb", padding: 12, borderRadius: 8 }}>
      <h2 style={{ marginTop: 0 }}>🛠 Debug: 口座検索</h2>

      <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
        <input
          placeholder="owner キー（ID/氏名の一部など）"
          value={key}
          onChange={(e) => setKey(e.target.value)}
          style={{ flex: 1, padding: 8 }}
        />
        <button onClick={onSearch} disabled={!key || loading}>
          {loading ? "検索中…" : "検索"}
        </button>
      </div>

      {error && <div style={{ color: "crimson" }}>{error}</div>}

      <div style={{ display: "grid", gap: 12 }}>
        <div>
          <h3>普通預金</h3>
          {!savings && <div>（未検索）</div>}
          {savings?.length === 0 && <div>該当なし</div>}
          <div style={{ display: "grid", gap: 8 }}>
            {savings?.map(a => (
              <Card key={`s-${a.id}`}>
                <div><b>{a.accountNo}</b> {a.ownerName}（{a.ownerId}）</div>
                <div>支店: {a.branchCode ?? "-"}</div>
                <div>残高: {a.balance ?? "-"}</div>
              </Card>
            ))}
          </div>
        </div>

        <div>
          <h3>定期預金</h3>
          {!tds && <div>（未検索）</div>}
          {tds?.length === 0 && <div>該当なし</div>}
          <div style={{ display: "grid", gap: 8 }}>
            {tds?.map(a => (
              <Card key={`t-${a.id}`}>
                <div><b>{a.accountNo}</b> {a.ownerName}（{a.ownerId}）</div>
                <div>期間(月): {a.termMonths ?? "-"}</div>
                <div>金額: {a.amount ?? "-"}</div>
                <div>開始日: {a.startDate ?? "-"}</div>
                <div>満期日: {a.maturityDate ?? "-"}</div>
                <div>金利: {a.interestRate ?? "-"}</div>
              </Card>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
