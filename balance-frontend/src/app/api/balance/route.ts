import { cookies } from "next/headers";

const BACKEND = process.env.BACKEND_BASE_URL ?? "http://localhost:8000";

type RawSavings = {
  id?: string;
  accountNo?: string | null;
  ownerKey?: string | null;
  balance?: number | null;
};

type RawTerm = {
  id?: string;
  accountNo?: string | null;
  ownerKey?: string | null;
  principal?: number | null;
  balance?: number | null;
};

type RawResponse = {
  owner: string;
  savings?: RawSavings[];
  timeDeposits?: RawTerm[];
};

type Normalized = Array<{
  kind: "savings" | "term";
  amount: number;
  accountId?: string;
}>;

function toNumber(x: any): number {
  const n = typeof x === "number" ? x : Number(x);
  return Number.isFinite(n) ? n : 0;
}

function normalize(raw: RawResponse): { owner: string; data: Normalized } {
  const out: Normalized = [];

  // 普通預金: savings[].balance
  for (const s of raw.savings ?? []) {
    out.push({
      kind: "savings",
      amount: toNumber(s.balance),
      accountId: s.accountNo ?? s.id,
    });
  }

  // 定期預金: timeDeposits[].balance があればそれを、無ければ principal
  for (const t of raw.timeDeposits ?? []) {
    const amount = t.balance != null ? toNumber(t.balance) : toNumber(t.principal);
    out.push({
      kind: "term",
      amount,
      accountId: t.accountNo ?? t.id,
    });
  }

  return { owner: raw.owner, data: out };
}

export async function GET() {
  const cookieStore = await cookies();
  const at = cookieStore.get("access_token")?.value;
  const owner = cookieStore.get("username")?.value;

  if (!owner) {
    return Response.json({ error: "not signed in" }, { status: 401 });
  }
  if (!at) {
    return Response.json({ error: "no access_token" }, { status: 401 });
  }

  const url = `${BACKEND}/balance-inquiry/${encodeURIComponent(owner)}`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${at}` },
    cache: "no-store",
  });

  const bodyText = await res.text();
  let raw: RawResponse;
  try {
    raw = JSON.parse(bodyText);
  } catch {
    return Response.json(
      { error: "invalid JSON from upstream", detail: bodyText },
      { status: 502 }
    );
  }

  if (!res.ok) {
    return Response.json(
      { error: `upstream ${res.status}`, detail: raw },
      { status: 502 }
    );
  }

  const normalized = normalize(raw);
  return Response.json(normalized, { status: 200 });
}
