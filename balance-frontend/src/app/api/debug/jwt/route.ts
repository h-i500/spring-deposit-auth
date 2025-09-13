// src/app/api/debug/jwt/route.ts
import { NextRequest, NextResponse } from "next/server";
export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  const at = req.cookies.get("access_token")?.value;
  if (!at) return NextResponse.json({ error: "no token" }, { status: 401 });
  const [h, p] = at.split(".");
  const header = JSON.parse(Buffer.from(h, "base64url").toString("utf8"));
  const payload = JSON.parse(Buffer.from(p, "base64url").toString("utf8"));
  // 最低限の可視化（機密は出さない）
  const pick = ((o: any, ks: string[]) => Object.fromEntries(ks.filter(k=>k in o).map(k=>[k,o[k]])));
  return NextResponse.json({
    header: pick(header, ["alg","kid"]),
    payload: pick(payload, ["iss","aud","azp","preferred_username","exp","iat","client_id","scope"]),
  });
}
