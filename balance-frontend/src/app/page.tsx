"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";

type Balance = {
  kind: "savings" | "term";
  amount: number;
  accountId?: string;
};

type BalanceResponse = {
  owner: string;
  data: Balance[];
};

type State =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "ok"; data: BalanceResponse }
  | { status: "error"; error: string };

function formatJPY(n: number) {
  return n.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

// 口座カード
function AccountCard({ kind, amount, owner, accountId }: Balance & { owner: string }) {
  const colorBox =
    kind === "savings"
      ? "bg-blue-50 border-blue-200"
      : "bg-emerald-50 border-emerald-200";

  const titleColor = kind === "savings" ? "text-blue-700" : "text-emerald-700";
  const valueColor = kind === "savings" ? "text-blue-900" : "text-emerald-900";

  return (
    <div className={`rounded-2xl border p-4 shadow-sm ${colorBox}`}>
      <h3 className={`font-semibold text-lg ${titleColor}`}>
        {kind === "savings" ? "普通預金" : "定期預金"}
      </h3>
      <p className={`text-2xl font-bold ${valueColor}`}>
        {formatJPY(amount)} <span className="text-base font-medium">円</span>
      </p>
      <p className="text-sm text-gray-700 mt-1">Owner: {owner}</p>
      {accountId && <p className="text-xs text-gray-500">Account ID: {accountId}</p>}
    </div>
  );
}

// Cookie から username を取り出す（簡易版）
function getUsernameFromCookie(): string | null {
  if (typeof document === "undefined") return null;
  const m = document.cookie.match(/(?:^|;\s*)username=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}

export default function Page() {
  const [state, setState] = useState<State>({ status: "idle" });
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    setUsername(getUsernameFromCookie());
  }, []);

  const login = () => (location.href = "/api/auth/login");
  const logout = async () => {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      location.reload();
    }
  };

  const fetchBalance = async () => {
    if (!username) {
      setState({ status: "error", error: "ログインしてください" });
      return;
    }
    try {
      setState({ status: "loading" });
      const res = await fetch("/api/balance", { cache: "no-store" });
      const json = (await res.json()) as BalanceResponse;
      if (!res.ok) throw new Error((json as any).error ?? `HTTP ${res.status}`);
      setState({ status: "ok", data: json });
    } catch (e: any) {
      setState({ status: "error", error: e.message ?? "failed" });
    }
  };

  const owner = state.status === "ok" ? state.data.owner : username ?? "";

  return (
    <main className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-5xl px-4 sm:px-6 py-8 sm:py-10 space-y-6">
        {/* ヘッダー */}
        <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <h1 className="text-2xl font-bold tracking-tight">Balance Inquiry</h1>
          <div className="flex items-center gap-3">
            {!username ? (
              <Button onClick={login}>Sign in</Button>
            ) : (
              <>
                <span className="text-sm text-gray-600">
                  Signed in as <b className="text-gray-900">{username}</b>
                </span>
                <Button variant="secondary" onClick={logout}>Logout</Button>
              </>
            )}
          </div>
        </header>

        {/* メインカード */}
        <section className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold">残高照会</h2>
              <p className="text-gray-600 text-sm">
                OWNER はログインユーザー名（
                <b className="text-gray-900">{username ?? "未ログイン"}</b>
                ）です。
              </p>
            </div>
            <div className="flex items-center gap-3">
              <Button onClick={fetchBalance} disabled={!username || state.status === "loading"}>
                {state.status === "loading" ? "Checking..." : "Check Balance"}
              </Button>
            </div>
          </div>

          {/* 状態表示 */}
          <div className="mt-6 space-y-3">
            {state.status === "idle" && (
              <div className="text-gray-500 text-sm">「Check Balance」を押すと結果が表示されます。</div>
            )}
            {state.status === "loading" && (
              <div className="inline-flex items-center gap-2 text-gray-600 text-sm" role="status" aria-live="polite">
                <svg className="size-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                  <path className="opacity-75" d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round"/>
                </svg>
                Loading...
              </div>
            )}
            {state.status === "error" && (
              <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-800 text-sm">
                {state.error}
              </div>
            )}
            {state.status === "ok" && (
              <div className="inline-flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-emerald-800 text-sm">
                <svg className="size-4" viewBox="0 0 24 24" fill="none">
                  <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                取得に成功しました
              </div>
            )}
          </div>

          {/* 結果カード */}
          {state.status === "ok" && (
            <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
              {state.data.data.map((b, i) => (
                <AccountCard key={i} {...b} owner={owner} />
              ))}
            </div>
          )}

          {/* デバッグ（開発時のみ） */}
          {process.env.NODE_ENV !== "production" && state.status === "ok" && (
            <pre className="mt-6 text-xs text-gray-500 bg-gray-100 p-3 rounded overflow-auto">
              {JSON.stringify(state.data, null, 2)}
            </pre>
          )}
        </section>
      </div>
    </main>
  );
}
