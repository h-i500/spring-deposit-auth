import { useCallback, useEffect, useState } from "react";

type Me = {
  principal?: string;
  preferred_username?: string;
  email?: string;
  [k: string]: any;
} | null;

type Probe = {
  url: string;
  status: number;
  contentType: string;
  bodyHead: string; // 先頭数百文字だけ
};

export default function LoginPanel() {
  const [me, setMe] = useState<Me>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [probe, setProbe] = useState<Probe | null>(null);

  const fetchMe = useCallback(async () => {
    setLoading(true);
    setErr(null);
    setProbe(null);
    try {
      // ★ 直で /secure/me を叩く（Cookie Path が /secure でも確実に一致）
      const url = `/secure/me?t=${Date.now()}`;
      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: {
          Accept: "application/json",
          "Cache-Control": "no-cache",
          Pragma: "no-cache",
        },
        redirect: "follow",
      });

      const contentType = res.headers.get("content-type") || "";
      const raw = await res.text();
      setProbe({
        url,
        status: res.status,
        contentType,
        bodyHead: raw.slice(0, 400),
      });

      if (res.status === 200 && contentType.includes("application/json")) {
        // JSON だったらログイン中
        try {
          const data = raw ? JSON.parse(raw) : {};
          setMe(data);
        } catch {
          // JSON パースできなければ未ログイン扱い
          setMe(null);
        }
      } else if (res.status === 401) {
        // 401 は未ログイン
        setMe(null);
      } else {
        // 302追従後にHTMLが返った・他のコード等は未ログイン扱い
        setMe(null);
      }
    } catch (e: any) {
      setErr(e?.message ?? String(e));
      setMe(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMe();
  }, [fetchMe]);

  const login = () => {
    // Kong が /auth/* → /secure/* へ付け替え
    window.location.href = "/auth/login";
  };

  // const logout = () => {
  //   // SecureResource の /secure/logout は /q/oidc/logout へ 303
  //   // 直接 /q/oidc/logout を叩いてもOK（kong でルート定義済）
  //   window.location.href = "/auth/logout";
  // };
  const logout = () => {
    // 絶対URLを安全に組み立て（/auth 混入を回避）
    const url = new URL("/secure/logout", window.location.origin);
    window.location.assign(url.toString());
  };

  return (
    <section className="card">
      <h2>認証（ログイン／ログアウト）</h2>

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
                <div>{me.preferred_username ?? me.principal ?? "-"}</div>
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

      {/* 診断情報（何が返ってきているか一目でわかる） */}
      {probe && (
        <details style={{ marginTop: 8 }}>
          <summary>診断情報（/secure/me のレスポンス）</summary>
          <pre>{JSON.stringify(probe, null, 2)}</pre>
        </details>
      )}

      {err && <pre style={{ color: "crimson" }}>{err}</pre>}

      <p className="hint">
        ※ XHR は <code>credentials: "include"</code> で送信しています。レスポンスが 200 かつ
        <code>application/json</code> の場合のみ「ログイン中」と判定します。
      </p>
    </section>
  );
}
