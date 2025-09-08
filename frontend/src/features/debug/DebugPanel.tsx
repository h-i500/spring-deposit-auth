import { useState } from "react";
import { fetchSavingsByOwner, fetchTimeDepositsByOwner, fetchOwners } from "../../api/debug";
// import { fetchSavingsByOwner, fetchTimeDepositsByOwner } from "../../api/debug";
import type { SavingsAccountDto, TimeDepositDto } from "../../types/account";
import Card from "../../components/Card";

export default function DebugPanel() {
  const [key, setKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [savings, setSavings] = useState<SavingsAccountDto[] | null>(null);
  const [tds, setTds] = useState<TimeDepositDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [owners, setOwners] = useState<string[] | null>(null);
  const [ownersLoading, setOwnersLoading] = useState(false);

  const fmt = (s?: string) => (s ? new Date(s).toLocaleString() : "-");

  // const onSearch = async () => {
  //   setLoading(true); setError(null);
  //   try {
  //     const [s, t] = await Promise.all([
  //       fetchSavingsByOwner(key),
  //       fetchTimeDepositsByOwner(key),
  //     ]);
  //     setSavings(s); setTds(t);
  //   } catch (e: any) {
  //     setError(e?.message ?? "検索に失敗しました");
  //   } finally {
  //     setLoading(false);
  //   }
  // };

  const onSearch = async (ownerKey?: string) => {
    const k = (ownerKey ?? key).trim();
    if (!k) return;                 // 空は何もしない
    setLoading(true); setError(null);
    try {
      const [s, t] = await Promise.all([
        fetchSavingsByOwner(k),
        fetchTimeDepositsByOwner(k),
      ]);
      setSavings(s); setTds(t);
    } catch (e: any) {
      setError(e?.message ?? "検索に失敗しました");
    } finally {
      setLoading(false);
    }
  };

  const onLoadOwners = async () => {
    setOwnersLoading(true); setError(null);
    try {
      const list = await fetchOwners();
      setOwners(list);
    } catch (e: any) {
      setError(e?.message ?? "owner一覧の取得に失敗しました");
    } finally {
      setOwnersLoading(false);
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
        {/* <button onClick={onSearch} disabled={!key || loading}>
          {loading ? "検索中…" : "検索"}
        </button> */}

        <button onClick={() => onSearch()} disabled={!key || loading}>
          {loading ? "検索中…" : "検索"}
        </button>

        {/* owner一覧ボタン */}
        <button onClick={onLoadOwners} disabled={ownersLoading}>
          {ownersLoading ? "取得中…" : "owner一覧"}
        </button>
      </div>

      {/* owner一覧の表示（クリックで検索） */}
      {owners && (
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
          {owners.length === 0 && <span>オーナーなし</span>}
          {owners.map(o => (
            <button
              key={o}
              onClick={() => { setKey(o); onSearch(o); }}   // ← これでエラー解消
              style={{ padding:"4px 8px", borderRadius:999, border:"1px solid #ccc", background:"#f9f9f9", cursor:"pointer" }}
              title={`"${o}" で検索`}
            >
              {o}
            </button>
          ))}
        </div>
      )}

      {error && <div style={{ color: "crimson" }}>{error}</div>}

      <div style={{ display: "grid", gap: 12 }}>
        <div>
          <h3>普通預金</h3>
          {!savings && <div>（未検索）</div>}
          {savings?.length === 0 && <div>該当なし</div>}
          <div style={{ display: "grid", gap: 8 }}>
            {savings?.map((a) => (
              <Card key={`s-${a.id}`}>
                <div>
                  <b>{a.accountNo ?? a.id}</b>{" "}
                  {/* ownerName / ownerId が無ければ owner 単体を使う */}
                  {a.ownerName ?? a.owner ?? "-"}
                  {a.ownerId ? `（${a.ownerId}）` : ""}
                </div>
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
            {tds?.map(t => (
              <Card key={`t-${t.id}`}>
                <div><b>{t.id}</b> {t.owner}</div>
                <div>期間(月): {t.termMonths ?? "-"}</div>
                <div>金額: {t.principal?.toLocaleString?.() ?? t.principal ?? "-"}</div>
                <div>開始日: {fmt(t.startAt)}</div>
                <div>満期日: {fmt(t.maturityAt)}</div>
                <div>金利: {t.interestRate ?? "-"}</div>
                <div>状態: {t.status ?? "-"}</div>
              </Card>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
