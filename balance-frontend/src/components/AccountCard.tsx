type Kind = "savings" | "term";
const palettes: Record<Kind, string> = {
  savings: "border-blue-200 bg-white",
  term:    "border-emerald-200 bg-white",
};
const ribbons: Record<Kind, string> = {
  savings: "bg-blue-600 text-white",
  term:    "bg-emerald-600 text-white",
};

export function AccountCard({
  kind, owner, balance, accountId
}: { kind: Kind; owner: string; balance: number; accountId?: string }) {
  return (
    <div className={`rounded-2xl border shadow-sm ${palettes[kind]}`}>
      <div className={`rounded-t-2xl px-4 py-2 ${ribbons[kind]}`}>
        <div className="text-sm font-semibold">
          {kind === "savings" ? "普通預金" : "定期預金"}
        </div>
      </div>
      <div className="p-4 space-y-1">
        <div className="text-xs text-gray-500">OWNER</div>
        <div className="text-sm font-medium">{owner}</div>
        {accountId && (
          <>
            <div className="text-xs text-gray-500 mt-3">ACCOUNT ID</div>
            <div className="text-sm font-mono">{accountId}</div>
          </>
        )}
        <div className="text-xs text-gray-500 mt-3">BALANCE</div>
        <div className="text-2xl font-bold tracking-tight">
          {balance.toLocaleString()} <span className="text-base font-semibold">円</span>
        </div>
      </div>
    </div>
  );
}
