// src/app/api/auth/callback/route.ts
import { NextRequest, NextResponse } from "next/server";
import { endpoints, clientId, redirectUri } from "@/lib/oidc";

// ★ Node ランタイムを明示（Edge にならないように）
export const runtime = "nodejs";

// ★ Node で atob は不可。Buffer(base64url) でOK
function decodeJwtPayload(jwt: string) {
  const payload = jwt.split(".")[1] ?? "";
  const json = Buffer.from(payload, "base64url").toString("utf8");
  return JSON.parse(json);
}

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");

  const cookieState = req.cookies.get("oauth_state")?.value;
  const verifier = req.cookies.get("pkce_verifier")?.value;

  // ★ リダイレクトは「絶対URL」にする
  if (!code || !state || !cookieState || state !== cookieState || !verifier) {
    return NextResponse.redirect(new URL("/", req.url), { status: 302 });
  }

  try {
    const body = new URLSearchParams({
      grant_type: "authorization_code",
      code,
      client_id: clientId,
      code_verifier: verifier,
      redirect_uri: redirectUri,
    });

    const tokenResp = await fetch(endpoints.token, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      cache: "no-store",
    });

    const json = await tokenResp.json();
    if (!tokenResp.ok) {
      console.error("Token error", json);
      return NextResponse.redirect(new URL("/?error=token", req.url), { status: 302 });
    }

    const accessToken: string = json.access_token;
    const idToken: string = json.id_token ?? "";
    const refreshToken: string | undefined = json.refresh_token;
    const expiresIn: number = json.expires_in ?? 900;

    const payload = idToken ? decodeJwtPayload(idToken) : {};
    const username = (payload as any).preferred_username || "";

    // ★ 絶対URLでトップへ
    const res = NextResponse.redirect(new URL("/", req.url), { status: 302 });
    const secure = process.env.NODE_ENV === "production";

    res.cookies.set("access_token", accessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: expiresIn });
    if (idToken) res.cookies.set("id_token", idToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: expiresIn });
    if (refreshToken) res.cookies.set("refresh_token", refreshToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 7 * 24 * 3600 });

    if (username) res.cookies.set("username", username, { httpOnly: false, sameSite: "lax", secure, path: "/", maxAge: expiresIn });

    // one-time 値は削除
    res.cookies.set("pkce_verifier", "", { path: "/", maxAge: 0 });
    res.cookies.set("oauth_state", "", { path: "/", maxAge: 0 });

    return res;
  } catch (e) {
    console.error(e);
    // ★ エラー時も絶対URLで
    return NextResponse.redirect(new URL("/?error=exception", req.url), { status: 302 });
  }
}
