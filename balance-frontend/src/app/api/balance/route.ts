import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  const access = req.cookies.get("access_token")?.value;
  const username = req.cookies.get("username")?.value || "";
  if (!access) return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  if (!username) return NextResponse.json({ error: "No username in session" }, { status: 400 });

  const base = process.env.BACKEND_BASE_URL!;
  try {
    const resp = await fetch(`${base}/balance-inquiry/${encodeURIComponent(username)}`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${access}`,
        Accept: "application/json",
      },
      cache: "no-store",
    });
    const text = await resp.text();
    let data: unknown;
    try { data = JSON.parse(text); } catch { data = { raw: text }; }
    return NextResponse.json(data, { status: resp.status });
  } catch (e: any) {
    return NextResponse.json({ error: e?.message ?? "Upstream fetch failed" }, { status: 502 });
  }
}
