<!--
PR タイトルは Conventional Commits を推奨:
feat(frontend): ログイン失敗時にトースト表示
fix(api): 入金APIで400が返る問題を修正
chore: 依存パッケージを更新
-->

## 目的 / 背景
- なぜこの変更が必要か（課題/要求）
- ユーザー/ビジネス/技術的な意図

## 変更内容（概要）
- 主要な変更点を箇条書き
- 影響範囲（frontend / backend / infra 等）

## 動作確認手順（ローカル）
```bash
# ビルド＆起動
cd frontend
npm ci
npm run build
cd ..
docker compose up -d --build

# アクセス URL / 動作確認観点を明記
# 例: http://localhost:8080 でログインフォームを開き、誤ったパスワードでエラートーストが出ること
