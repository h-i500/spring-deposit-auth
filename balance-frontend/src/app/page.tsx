// "use client";

// import { useEffect, useState } from "react";

// type S = { status: "idle" } | { status: "loading" } | { status: "ok"; data: unknown } | { status: "err"; error: string };

// export default function Home() {
//   const [username, setUsername] = useState<string | null>(null);
//   const [s, setS] = useState<S>({ status: "idle" });

//   useEffect(() => {
//     const m = document.cookie.match(/(?:^|;\s*)username=([^;]+)/);
//     setUsername(m ? decodeURIComponent(m[1]) : null);
//   }, []);

//   function login() {
//     // GET /api/auth/login で Keycloak へリダイレクト
//     window.location.href = "/api/auth/login";
//   }
//   async function logout() {
//     await fetch("/api/auth/logout", { method: "POST" });
//     location.reload();
//   }
//   async function check() {
//     setS({ status: "loading" });
//     try {
//       const res = await fetch("/api/balance");
//       const json = await res.json();
//       if (!res.ok) throw new Error(json.error ?? `HTTP ${res.status}`);
//       setS({ status: "ok", data: json });
//     } catch (e: any) {
//       setS({ status: "err", error: e?.message ?? "failed" });
//     }
//   }

//   return (
//     <main className="min-h-screen p-6">
//       <div className="mx-auto max-w-2xl space-y-4">
//         <header className="flex items-center justify-between">
//           <h1 className="text-2xl font-bold">Balance Inquiry</h1>
//           <div className="flex items-center gap-3">
//             {!username ? (
//               <button onClick={login} className="rounded-md px-4 py-2 bg-black text-white">Sign in</button>
//             ) : (
//               <>
//                 <span className="text-sm text-gray-600">Signed in as <b>{username}</b></span>
//                 <button onClick={logout} className="border rounded-md px-3 py-1">Logout</button>
//               </>
//             )}
//           </div>
//         </header>

//         {username && (
//           <section className="border rounded-xl p-4 space-y-3">
//             <p>OWNER はログインユーザー名（<b>{username}</b>）として照会します。</p>
//             <button onClick={check} className="rounded-md px-4 py-2 bg-black text-white" disabled={s.status==="loading"}>
//               {s.status === "loading" ? "Checking..." : "Check Balance"}
//             </button>

//             {s.status === "idle" && <p className="text-gray-500">結果はここに表示されます。</p>}
//             {s.status === "err" && <div className="text-red-700 border border-red-200 bg-red-50 rounded-md p-3">{s.error}</div>}
//             {s.status === "ok" && (
//               <pre className="text-sm border rounded-md p-3 overflow-auto bg-white">
//                 {JSON.stringify(s.data, null, 2)}
//               </pre>
//             )}
//           </section>
//         )}

//         {!username && (
//           <p className="text-gray-600">ログインすると残高照会ができるようになります。</p>
//         )}
//       </div>
//     </main>
//   );
// }


"use client";

import { useEffect, useState } from "react";

type S =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "ok"; data: any }
  | { status: "err"; error: string };

export default function Home() {
  const [username, setUsername] = useState<string | null>(null);
  const [s, setS] = useState<S>({ status: "idle" });

  useEffect(() => {
    const m = document.cookie.match(/(?:^|;\s*)username=([^;]+)/);
    setUsername(m ? decodeURIComponent(m[1]) : null);
  }, []);

  function login() {
    window.location.href = "/api/auth/login";
  }
  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    location.reload();
  }
  async function check() {
    setS({ status: "loading" });
    try {
      const res = await fetch("/api/balance");
      const json = await res.json();
      if (!res.ok) throw new Error(json.error ?? `HTTP ${res.status}`);
      setS({ status: "ok", data: json });
    } catch (e: any) {
      setS({ status: "err", error: e?.message ?? "failed" });
    }
  }

  return (
    <main className="min-h-screen">
      <div className="mx-auto max-w-3xl px-6 py-10 space-y-6">
        <header className="flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">Balance Inquiry</h1>
          <div className="flex items-center gap-3">
            {!username ? (
              <button
                onClick={login}
                className="rounded-lg px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 transition"
              >
                Sign in
              </button>
            ) : (
              <>
                <span className="text-sm text-gray-600">
                  Signed in as <b className="text-gray-900">{username}</b>
                </span>
                <button
                  onClick={logout}
                  className="rounded-lg px-3 py-2 border border-gray-300 text-gray-700 hover:bg-gray-100 transition"
                >
                  Logout
                </button>
              </>
            )}
          </div>
        </header>

        <section className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold mb-2">残高照会</h2>
          <p className="text-gray-600 mb-4">
            OWNER はログインユーザー名（<b className="text-gray-900">{username ?? "未ログイン"}</b>）を使用します。
          </p>

          <div className="flex items-center gap-3">
            <button
              onClick={check}
              disabled={!username || s.status === "loading"}
              className="rounded-lg px-4 py-2 bg-emerald-600 text-white disabled:opacity-50 hover:bg-emerald-700 transition"
            >
              {s.status === "loading" ? "Checking..." : "Check Balance"}
            </button>
            {!username && (
              <span className="text-sm text-red-700">
                先にログインしてください。
              </span>
            )}
          </div>

          {/* 結果表示 */}
          <div className="mt-6">
            {s.status === "idle" && (
              <div className="text-gray-500">結果はここに表示されます。</div>
            )}
            {s.status === "err" && (
              <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-800">
                {s.error}
              </div>
            )}
            {s.status === "ok" && (
              <div className="rounded-xl border border-gray-200 bg-gray-50">
                <div className="p-4 border-b border-gray-200">
                  <div className="text-sm text-gray-600">OWNER</div>
                  <div className="text-base font-semibold text-gray-900">
                    {username}
                  </div>
                </div>
                <pre className="p-4 text-sm text-gray-900 overflow-auto">
                  {JSON.stringify(s.data, null, 2)}
                </pre>
              </div>
            )}
          </div>
        </section>

        <footer className="text-xs text-gray-500">
          © Demo – Next.js + Tailwind + Keycloak
        </footer>
      </div>
    </main>
  );
}
