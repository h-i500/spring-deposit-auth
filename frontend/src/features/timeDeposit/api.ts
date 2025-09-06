// frontend/src/features/timeDeposit/api.ts

export type TimeDeposit = {
  id: string;
  principal: number;
  annualRate: number;
  maturityDate: string; // ISO
  status: "OPEN" | "CLOSED" | string;
  owner: string;
  termDays: number;
  startAt: string; // ISO
};

async function api<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const res = await fetch(input, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init?.headers || {}) },
    ...init,
  });
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`;
    try {
      const body = await res.text();
      if (body) msg += `\n${body}`;
    } catch {}
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as unknown as T);
}

/** 定期預金作成（普通から引落しあり） */
export function openDeposit(body: {
  owner: string;
  principal: number;
  annualRate: number;
  termDays: number;
  fromAccountId: string;
}) {
  return api<TimeDeposit>("/api/deposits", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** 満期解約（普通へ自動振替） */
export function closeDeposit(
  depositId: string,
  toAccountId: string,
  atISO: string
) {
  const url = `/api/deposits/${encodeURIComponent(
    depositId
  )}/close?toAccountId=${encodeURIComponent(
    toAccountId
  )}&at=${encodeURIComponent(atISO)}`;
  return api<{ payout: number; status: string; id: string; toAccountId: string }>(
    url,
    { method: "POST" }
  );
}


/** 定期預金の詳細取得（残高照会用）：GET /api/deposits/{id} */
export function getDepositById(id: string) {
  return api<TimeDeposit>(`/api/deposits/${encodeURIComponent(id)}`, {
    method: "GET",
  });
}