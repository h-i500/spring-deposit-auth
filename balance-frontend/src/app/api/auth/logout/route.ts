import { NextRequest, NextResponse } from "next/server";
import { endpoints } from "@/lib/oidc";

export async function POST(req: NextRequest) {
  const idToken = req.cookies.get("id_token")?.value;
  const postLogout = process.env.APP_BASE_URL!;
  const params = new URLSearchParams();
  if (idToken) params.set("id_token_hint", idToken);
  params.set("post_logout_redirect_uri", postLogout);

  const res = NextResponse.json({ ok: true });
  for (const k of ["access_token","id_token","refresh_token","username"]) {
    res.cookies.set(k, "", { path: "/", maxAge: 0 });
  }
  // Keycloakへもログアウトリダイレクトしたい場合は 204 ではなく Location 返してもOK
  res.headers.set("X-IdP-Logout", `${endpoints.logout}?${params.toString()}`);
  return res;
}
