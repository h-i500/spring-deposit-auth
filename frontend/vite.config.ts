import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// dev 時は BFF/Kong 等へプロキシ。必要に応じてターゲットを書き換え。
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/auth":     { target: "http://localhost:8000", changeOrigin: true },
      "/savings":  { target: "http://localhost:8000", changeOrigin: true },
      "/deposits": { target: "http://localhost:8000", changeOrigin: true },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
});
