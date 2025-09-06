// frontend/src/App.tsx
import React from "react";
import LoginPanel from "./features/auth/LoginPanel";
import SavingsPanel from "./features/savings/SavingsPanel";
import TimeDepositPanel from "./features/timeDeposit/TimeDepositPanel";
import DebugPanel from "./features/debug/DebugPanel";

export default function App() {
  return (
    <div
      style={{
        maxWidth: 1100,
        margin: "0 auto",
        padding: "16px",
        fontFamily:
          '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans JP", sans-serif',
      }}
    >
      <header style={{ marginBottom: 16 }}>
        <h1 style={{ margin: 0, fontSize: 22 }}>Spring Deposit Demo – WebApp</h1>
        <p style={{ margin: "4px 0 0", color: "#666" }}>
          ログイン後、普通預金（口座作成・入金・出金）と定期預金（作成・満期解約）が利用できます。
        </p>
      </header>

      <main style={{ display: "grid", gap: 16 }}>
        {/* 認証（ログイン／ログアウト＆ステータス） */}
        <LoginPanel />

        {/* 普通預金（口座作成／入金／出金） */}
        <SavingsPanel />

        {/* 定期預金（作成／満期解約） */}
        <TimeDepositPanel />

        {/* デバッグ: 開発時のみ表示 */}
        {/* {import.meta.env.DEV && <DebugPanel />} */}
        <DebugPanel />

      </main>

      <footer style={{ marginTop: 24, color: "#888", fontSize: 12 }}>
        <div>© {new Date().getFullYear()} spring-deposit-auth</div>
        <div>Path base: /app/（Kong 配下で配信）</div>
      </footer>
    </div>
  );
}
