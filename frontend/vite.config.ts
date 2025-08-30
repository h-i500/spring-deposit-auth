import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,

    // ★ Kong 経由で Host が "frontend" になるので許可する
    allowedHosts: ["frontend", "localhost", "127.0.0.1"],

    // ★ HMR の接続先をブラウザ視点（Kong 側）に合わせる
    hmr: {
      host: "localhost",   // ブラウザが見ているホスト（Kong 側）
      clientPort: 8000,    // ブラウザから見えるポート（Kong: 8000）
      protocol: "ws",
      path: "/@vite"
    }
  }
});
