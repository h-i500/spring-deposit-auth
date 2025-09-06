
export type SavingsAccountDto = {
  id: string;
  accountNo?: string;
  branchCode?: string;
  balance?: number;

  // 既存の想定（あれば使う）
  ownerId?: string;
  ownerName?: string;

  // debug API が owner だけ返すケースに備えたフォールバック
  owner?: string;
};


// 既存の export はそのままにして、TimeDepositDto だけ更新
export type TimeDepositDto = {
  id: string;
  owner: string;
  principal: number;
  status: 'OPEN' | 'CLOSED' | string;
  startAt?: string;       // ISO文字列
  maturityAt?: string;    // ISO文字列
  termMonths?: number;
  interestRate?: number;
};
