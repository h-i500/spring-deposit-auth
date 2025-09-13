// src/components/BalanceResult.tsx
export default function BalanceResult({ owner, balance }: { owner: string, balance: number }) {
  return (
    <div className="max-w-md mx-auto mt-6 p-6 bg-white rounded-xl shadow-md border border-gray-200">
      <h2 className="text-lg font-bold text-gray-800 mb-2">残高照会結果</h2>
      <p className="text-gray-700">
        <span className="font-semibold">口座名義:</span> {owner}
      </p>
      <p className="text-gray-700">
        <span className="font-semibold">残高:</span> {balance.toLocaleString()} 円
      </p>
    </div>
  );
}
