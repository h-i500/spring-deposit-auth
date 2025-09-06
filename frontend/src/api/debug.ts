// frontend/src/api/debug.ts
import { apiFetch } from "./client";
import type { SavingsAccountDto, TimeDepositDto } from "../types/account";

// export async function fetchSavingsByOwner(ownerKey: string): Promise<SavingsAccountDto[]> {
//   return apiFetch(`/debug/savings?ownerKey=${encodeURIComponent(ownerKey)}`);
// }

// export async function fetchTimeDepositsByOwner(ownerKey: string): Promise<TimeDepositDto[]> {
//   return apiFetch(`/debug/time-deposits?ownerKey=${encodeURIComponent(ownerKey)}`);
// }

export async function fetchSavingsByOwner(ownerKey: string) {
  return apiFetch(`/api/debug/savings?ownerKey=${encodeURIComponent(ownerKey)}`);
}
export async function fetchTimeDepositsByOwner(ownerKey: string) {
  return apiFetch(`/api/debug/time-deposits?ownerKey=${encodeURIComponent(ownerKey)}`);
}
