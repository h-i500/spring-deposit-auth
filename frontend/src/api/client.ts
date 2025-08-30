export const API_BASE = import.meta.env.VITE_API_BASE || "";

async function parse(res: Response) {
  const text = await res.text();
  try { return text ? JSON.parse(text) : {}; } catch { return { raw: text }; }
}

export async function apiFetch(path: string, init: RequestInit = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: "include",
    headers: { Accept: "application/json", ...(init.headers || {}) },
    ...init,
  });
  const data = await parse(res);
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`);
    (err as any).payload = data;
    throw err;
  }
  return data;
}
