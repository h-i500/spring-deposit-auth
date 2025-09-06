// frontend/src/lib/api.ts
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init.headers || {}) },
    ...init,
  });

  // 未ログインなら Keycloak に飛ばす 302 をそのままブラウザ遷移
  if (res.status === 302) {
    const loc = res.headers.get("Location");
    if (loc) window.location.href = loc;
    throw new Error("Redirecting to login…");
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()) as T;
}
