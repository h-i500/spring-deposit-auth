import { NextResponse } from "next/server";
import { createPkce, endpoints, clientId, redirectUri } from "@/lib/oidc";

export async function GET() {
  const { verifier, challenge } = await createPkce();
  const state = crypto.randomUUID();

  const params = new URLSearchParams({
    client_id: clientId,
    response_type: "code",
    scope: "openid profile email",
    redirect_uri: redirectUri,
    code_challenge_method: "S256",
    code_challenge: challenge,
    state,
  });

  const res = NextResponse.redirect(`${endpoints.auth}?${params.toString()}`, 302);
  const secure = process.env.NODE_ENV === "production";
  // PKCE/STATEを短命httpOnly Cookieに保存
  res.cookies.set("pkce_verifier", verifier, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 600 });
  res.cookies.set("oauth_state", state, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 600 });
  return res;
}
