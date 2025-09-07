import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// /app/ 配下で配信（Kong で /app にリバースプロキシ）
export default defineConfig({
  base: "/app/",
  // base: './',   // ← ここを './' に
  plugins: [react()],

  // 開発（npm run dev）時の API プロキシ：ローカルで直接叩く場合に使用
  server: {
    port: 5173,
    proxy: {
      "/auth":     { target: "http://localhost:8000", changeOrigin: true },
      "/savings":  { target: "http://localhost:8000", changeOrigin: true },
      "/deposits": { target: "http://localhost:8000", changeOrigin: true },
    },
  },

  // 本番配信（コンテナ内で vite preview を起動）時のホスト許可
  preview: {
    host: true,             // 0.0.0.0 で待ち受け
    port: 5173,
    // Kong 経由だと Host: frontend で到達するケースがあるので許可する
    // preserve_host の設定次第で localhost になることもあるため両方許可
    allowedHosts: ["frontend", "localhost", "127.0.0.1"],
  },

  build: {
    outDir: "dist",
    sourcemap: true,
  },
});
