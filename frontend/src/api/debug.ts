
import { apiFetch } from "./client";
import type { SavingsAccountDto, TimeDepositDto } from "../types/account";

export async function fetchSavingsByOwner(ownerKey: string): Promise<SavingsAccountDto[]> {
  const res = await fetch(`/api/debug/savings?ownerKey=${encodeURIComponent(ownerKey)}`, {
    credentials: "include",
  });
  if (!res.ok) throw new Error("普通預金の検索に失敗しました");

  const raw = (await res.json()) as any[];

  // snake_case も吸収して camel に寄せる
  return raw.map((x) => ({
    id: x.id ?? x.account_id ?? crypto.randomUUID(),
    accountNo: x.accountNo ?? x.account_no ?? x.no,
    branchCode: x.branchCode ?? x.branch_code ?? x.branch,
    balance: x.balance ?? x.amount,

    // あれば使う（なければ下の owner を UI 側で使う）
    ownerId: x.ownerId ?? x.owner_id,
    ownerName: x.ownerName ?? x.owner_name,

    // debug API が単一の owner を返す場合のフォールバック
    owner: x.owner,
  }));
}



export async function fetchTimeDepositsByOwner(ownerKey: string): Promise<TimeDepositDto[]> {
  const res = await fetch(`/api/debug/time-deposits?ownerKey=${encodeURIComponent(ownerKey)}`);
  const data = await res.json(); // [{ id, owner, principal, start_at, maturity_at, ... }]
  return data.map((r: any) => ({
    id: r.id,
    owner: r.owner,
    principal: r.principal,
    status: r.status,
    startAt: r.startAt ?? r.start_at,
    maturityAt: r.maturityAt ?? r.maturity_at,
    termMonths: r.termMonths ?? r.term_months,
    interestRate: r.interestRate ?? r.interest_rate,
  }));
}
