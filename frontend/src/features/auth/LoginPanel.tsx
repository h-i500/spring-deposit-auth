import { useEffect, useState } from "react";

type Me =
  | {
      name?: string;
      preferred_username?: string;
      email?: string;
      [k: string]: any;
    }
  | null;

export default function LoginPanel() {
  const [me, setMe] = useState<Me>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function fetchMe() {
    setErr(null);
    try {
      // ★ ログイン状態の判定は /secure/me を 200 判定で
      const res = await fetch("/secure/me", { credentials: "include" });
      if (res.status === 200) {
        const data = await res.json();
        setMe(data ?? {});
      } else if (res.status === 401) {
        setMe(null);
      } else {
        setMe(null);
        setErr(`/secure/me → HTTP ${res.status}`);
      }
    } catch (e: any) {
      setErr(e?.message ?? String(e));
      setMe(null);
    }
  }

  useEffect(() => {
    fetchMe();
  }, []);

  function login() {
    // BFF が /secure/login → Keycloak へ 302
    window.location.href = "/auth/login"; // kong が /secure/login へ付け替え
  }

  function logout() {
    // Quarkus OIDC のログアウトエンドポイント（kong で route 済）
    window.location.href = "/q/oidc/logout";
  }

  return (
    <>
      <div className="row">
        <div>
          <div className="field">
            <label>ログインステータス</label>
            <span className="badge">{me ? "ログイン中" : "未ログイン"}</span>
          </div>

          {me && (
            <>
              <div className="field">
                <label>ユーザー</label>
                <div>{me.preferred_username ?? me.name ?? "-"}</div>
              </div>
              {me.email && (
                <div className="field">
                  <label>Email</label>
                  <div>{me.email}</div>
                </div>
              )}
            </>
          )}
        </div>

        <div style={{ alignSelf: "end" }}>
          {me ? (
            <button onClick={logout} disabled={loading}>
              ログアウト
            </button>
          ) : (
            <button onClick={login} disabled={loading}>
              ログイン
            </button>
          )}
        </div>
      </div>

      <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
        <button onClick={fetchMe} disabled={loading}>
          ステータス再取得
        </button>
      </div>

      {err && <pre>{err}</pre>}
      <p className="hint">
        ※ セッション Cookie は <code>credentials: "include"</code> で送信しています。Kong 側は
        <code>/auth/* → /secure/*</code> の付け替え（strip_path:true）が設定済みです。
      </p>
    </>
  );
}
