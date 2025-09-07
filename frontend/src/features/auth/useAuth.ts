import { useEffect, useState } from "react";

export type UserInfo = { name?: string; email?: string } | null;

export function useAuth() {
  const [user, setUser] = useState<UserInfo>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await fetch("/secure/me", { credentials: "include", redirect: "manual" });
        if (res.ok) {
          const data = await res.json().catch(() => ({}));
          setUser(data || {});
        } else {
          setUser(null);
        }
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const promptLogin = () => {
    // 自動 POST しない方針なので、単にログイン画面へ誘導するだけ
    window.location.href = "/auth/login"; // or "/secure/login"（Kong/BFF の公開パスに合わせて）
  };

  return { user, loading, promptLogin, isAuthenticated: !!user };
}
