"use client";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { AccountCard } from "@/components/AccountCard";
import { Spinner, AlertError, Success } from "@/components/Feedback";

type S = { status: "idle"|"loading"|"ok"|"err"; data?: any; error?: string };

export default function Home() {
  const [username, setUsername] = useState<string | null>(null);
  const [s, setS] = useState<S>({ status: "idle" });

  useEffect(() => {
    const m = document.cookie.match(/(?:^|;\s*)username=([^;]+)/);
    setUsername(m ? decodeURIComponent(m[1]) : null);
  }, []);

  const login = () => (location.href = "/api/auth/login");
  const logout = async () => { await fetch("/api/auth/logout",{method:"POST"}); location.reload(); };
  const check = async () => {
    setS({ status: "loading" });
    try {
      const res = await fetch("/api/balance");
      const json = await res.json();
      if (!res.ok) throw new Error(json.error ?? `HTTP ${res.status}`);
      setS({ status: "ok", data: json });
    } catch (e:any) { setS({ status:"err", error: e.message }); }
  };

  const balances = s.status==="ok" ? (Array.isArray(s.data) ? s.data : [s.data]) : [];

  return (
    <main className="min-h-screen">
      <div className="mx-auto max-w-5xl px-4 sm:px-6 py-8 sm:py-10 space-y-6">
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

        <section className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold">残高照会</h2>
              <p className="text-gray-600 text-sm">OWNER はログインユーザー名（<b className="text-gray-900">{username ?? "未ログイン"}</b>）。</p>
            </div>
            <div className="flex items-center gap-3">
              <Button onClick={check} disabled={!username || s.status==="loading"}>
                {s.status==="loading" ? "Checking..." : "Check Balance"}
              </Button>
            </div>
          </div>

          <div className="mt-6 space-y-3">
            {s.status==="idle" && <div className="text-gray-500 text-sm">結果はここに表示されます。</div>}
            {s.status==="loading" && <Spinner/>}
            {s.status==="err" && <AlertError>{s.error}</AlertError>}
            {s.status==="ok" && <Success>取得に成功しました</Success>}
          </div>

          {s.status==="ok" && (
            <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* ここはあなたの API 仕様に合わせて map してください */}
              {balances.map((b:any, i:number) => (
                <AccountCard
                  key={i}
                  kind={i===0 ? "savings" : "term"}
                  owner={username ?? ""}
                  balance={Number(b.balance ?? b.amount ?? 0)}
                  accountId={b.accountId ?? b.id}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
