// frontend/src/features/timeDeposit/TimeDepositPanel.tsx
import { useMemo, useState } from "react";
import { closeDeposit, openDeposit, getDepositById, type TimeDeposit } from "./api";
import { getAccountById } from "../savings/api";


export default function TimeDepositPanel() {
  // 作成フォーム
  const [owner, setOwner] = useState("Hanako");
  const [principal, setPrincipal] = useState("10000");
  const [annualRate, setAnnualRate] = useState("0.015");
  const [termDays, setTermDays] = useState("30");
  const [fromAccountId, setFromAccountId] = useState("");

  // 解約フォーム
  const [depositId, setDepositId] = useState("");
  const [toAccountId, setToAccountId] = useState("");
  const [atISO, setAtISO] = useState("");

  // 結果や状態
  const [lastCreated, setLastCreated] = useState<TimeDeposit | null>(null);
  const [lastClosed, setLastClosed] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 追加: 定期預金の残高照会用
  const [inqLoading, setInqLoading] = useState(false);
  const [inqErr, setInqErr] = useState<string | null>(null);
  const [inquired, setInquired] = useState<TimeDeposit | null>(null);

  const maturityISO = useMemo(() => lastCreated?.maturityDate ?? "", [lastCreated]);

  function fillNow() {
    setAtISO(new Date().toISOString());
  }
  function fillMaturity() {
    if (maturityISO) setAtISO(maturityISO);
  }
  function useLastIds() {
    if (lastCreated?.id) setDepositId(lastCreated.id);
    if (fromAccountId) setToAccountId(fromAccountId);
  }

  // 追加: 定期預金の残高照会（depositIdベース）
  async function onInquiryTD() {
    const id = depositId.trim();
    if (!id) return;
    setInqErr(null);
    setInqLoading(true);
    try {
      const td = await getDepositById(id);
      setInquired(td);
    } catch (e: any) {
      setInqErr(e?.message ?? String(e));
      setInquired(null);
    } finally {
      setInqLoading(false);
    }
  }


  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const body = {
        owner: owner.trim(),
        principal: Number(principal),
        annualRate: Number(annualRate),
        termDays: Number(termDays),
        fromAccountId: fromAccountId.trim(),
      };
      if (!body.owner || !body.fromAccountId) throw new Error("owner / fromAccountId を入力してください。");
      if (!(body.principal > 0)) throw new Error("principal は正の数で入力してください。");
      if (!(body.annualRate > 0)) throw new Error("annualRate は正の数で入力してください。");
      if (!(body.termDays > 0)) throw new Error("termDays は正の整数で入力してください。");

      const res = await openDeposit(body);
      setLastCreated(res);
      setDepositId(res.id);
      setAtISO(res.maturityDate); // 便宜上、満期日時を解約フォームにセット
      setLastClosed(null);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }

  async function onClose(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      if (!depositId.trim()) throw new Error("depositId を入力してください。");
      if (!toAccountId.trim()) throw new Error("toAccountId を入力してください。");
      const at = (atISO || "").trim();
      if (!at) throw new Error("at（ISO 形式）を入力してください。例: " + new Date().toISOString());

      const res = await closeDeposit(depositId.trim(), toAccountId.trim(), at);
      setLastClosed(res);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="card">
      <h3>定期預金</h3>

      {/* 作成 */}
      <form onSubmit={onCreate} style={{ display: "grid", gap: 8 }}>
        <strong>定期預金作成（普通から引落し）</strong>
        <div style={{ display: "grid", gap: 8, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
          <label>
            owner
            <input value={owner} onChange={(e) => setOwner(e.target.value)} placeholder="Hanako" required />
          </label>
          <label>
            principal
            <input type="number" min={1} step={1} value={principal} onChange={(e) => setPrincipal(e.target.value)} required />
          </label>
          <label>
            annualRate
            <input type="number" min={0} step="0.0001" value={annualRate} onChange={(e) => setAnnualRate(e.target.value)} required />
          </label>
          <label>
            termDays
            <input type="number" min={1} step={1} value={termDays} onChange={(e) => setTermDays(e.target.value)} required />
          </label>
          <label style={{ gridColumn: "1 / -1" }}>
            fromAccountId
            <input value={fromAccountId} onChange={(e) => setFromAccountId(e.target.value)} placeholder="普通口座ID" required />
          </label>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button type="submit" disabled={loading}>作成</button>
          {lastCreated && (
            <span style={{ color: "#555" }}>
              作成済み: <code style={{ userSelect: "all" }}>{lastCreated.id}</code>
              {" "}（満期: {new Date(lastCreated.maturityDate).toLocaleString()}）
            </span>
          )}
        </div>
      </form>

      {/* 解約 */}
      <form onSubmit={onClose} style={{ display: "grid", gap: 8, marginTop: 12 }}>
        <strong>満期解約（普通へ自動振替）</strong>
        <div style={{ display: "grid", gap: 8, gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))" }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <label>
              depositId
              <input
                value={depositId}
                onChange={(e) => setDepositId(e.target.value)}
                placeholder="定期預金ID"
                required
              />
            </label>
              <label>
              toAccountId
              <input value={toAccountId} onChange={(e) => setToAccountId(e.target.value)} placeholder="振替先 普通口座ID" required />
            </label>
            <button type="button" onClick={onInquiryTD} disabled={inqLoading || !depositId.trim()}>
              {inqLoading ? "照会中..." : "残高照会（定期）"}
            </button>
            {inquired && (
              <span style={{ color: "#555" }}>
                元本: {inquired.principal.toLocaleString()} 円 / ステータス: {inquired.status}
                {" "}（満期: {new Date(inquired.maturityDate).toLocaleString()}）
              </span>
            )}
          </div>
          <label style={{ gridColumn: "1 / -1" }}>
            at（ISO 形式）
            <input
              value={atISO}
              onChange={(e) => setAtISO(e.target.value)}
              placeholder={new Date().toISOString()}
              required
            />
          </label>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" disabled={loading}>解約実行</button>
          <button type="button" onClick={fillNow} disabled={loading}>現在時刻をセット</button>
          <button type="button" onClick={fillMaturity} disabled={loading || !maturityISO}>満期日時をセット</button>
          <button type="button" onClick={useLastIds} disabled={loading || !lastCreated}>作成結果のIDを反映</button>
        </div>
        {lastClosed && (
          <div style={{ marginTop: 6, color: "#555" }}>
            解約結果: payout={lastClosed.payout} / status={lastClosed.status} / to={lastClosed.toAccountId}
          </div>
        )}
      </form>

      {(err || inqErr) && (
        <pre style={{ color: "crimson", whiteSpace: "pre-wrap", marginTop: 8 }}>{err ?? inqErr}</pre>
      )}
    </section>
  );
}
