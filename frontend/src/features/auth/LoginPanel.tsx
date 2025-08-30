import { useEffect, useState } from "react";
import { apiFetch, API_BASE } from "../../api/client";
import Card from "../../components/Card";

type Session = { authenticated?: boolean; user?: { name?: string; [k: string]: any } } | null;

export default function LoginPanel() {
  const [session, setSession] = useState<Session>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function load() {
    setErr(null);
    try {
      // 既存実装に合わせて /auth/session を想定（なければ 200/401 のどちらかで返る API に差し替えてください）
      const s = await apiFetch("/auth/session").catch(async (e) => {
        // 401 などの場合は未ログインとして扱う
        if ((e as any).message?.includes("HTTP 401")) return { authenticated: false };
        throw e;
      });
      setSession(s);
    } catch (e: any) {
      setErr(e?.payload ? JSON.stringify(e.payload) : e.message);
    }
  }

  useEffect(() => { load(); }, []);

  function login() {
    // Keycloak 等のリダイレクト型ログインを想定
    window.location.href = API_BASE + "/auth/login";
  }

  async function logout() {
    setLoading(true);
    setErr(null);
    try {
      await apiFetch("/auth/logout", { method: "POST" });
      await load();
    } catch (e: any) {
      setErr(e?.payload ? JSON.stringify(e.payload) : e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="row">
        <div>
          <div className="field">
            <label>ステータス</label>
            <span className="badge">
              {session?.authenticated ? "ログイン中" : "未ログイン"}
            </span>
          </div>
          {session?.user?.name && (
            <div className="field">
              <label>ユーザー</label>
              <div>{session.user.name}</div>
            </div>
          )}
        </div>
        <div style={{ alignSelf: "end" }}>
          {session?.authenticated ? (
            <button onClick={logout} disabled={loading}>
              {loading ? "ログアウト中..." : "ログアウト"}
            </button>
          ) : (
            <button onClick={login}>ログイン</button>
          )}
        </div>
      </div>

      {err && <pre>{err}</pre>}
      <p className="hint">※ エンドポイントが異なる場合は <code>LoginPanel.tsx</code> 内のパスを調整してください。</p>
    </>
  );
}
