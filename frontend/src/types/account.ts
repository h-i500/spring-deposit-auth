export interface SavingsAccountDto {
  id: number;
  accountNo: string;
  ownerId: string;
  ownerName: string;
  branchCode?: string;
  balance?: number;
}

export interface TimeDepositDto {
  id: number;
  accountNo: string;
  ownerId: string;
  ownerName: string;
  termMonths?: number;
  amount?: number;
  startDate?: string;
  maturityDate?: string;
  interestRate?: number;
}
