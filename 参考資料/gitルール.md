
---

# ブランチ保護設定案（`main`）

> **狙い**：`main` を常にデプロイ可能に保ち、PR 経由のレビューとコンフリクト解消を強制。CI が無い現状でも「レビュー」「会話解決」「最新化」を守れば事故は大幅に減らせます。  
> CI 導入後は「必須ステータスチェック」を追加する前提で拡張可能にします。

## 推奨ルール
1. **Require a pull request before merging**（マージ前に PR を必須）
   - Require approvals: **1 名以上**
   - Dismiss stale approvals when new commits are pushed: **ON**（再レビューを促す）
   - Require review from Code Owners: **ON**（CODEOWNERS を後述）
   - Require approval of the most recent reviewable push: **ON**
2. **Require conversation resolution before merging**: **ON**（未解決のコメントが残っているとマージ不可）
3. **Require branches to be up to date before merging**: **ON**  
   → base（main）が進んだら rebase/merge して最新化してからマージ
4. **Allow force pushes**: **OFF**（main には禁止）
5. **Allow deletions**: **OFF**（main 削除禁止）
6. **Restrict who can push to matching branches**: **ON（必要に応じて）**  
   → Admin/Release Maintainers のみに限定、基本は PR 経由
7. **Merge 方法**（リポジトリ設定 > Merge button）
   - **Squash merge: ON**（推奨）  
   - Merge commit: OFF（履歴ノイズ抑制のため）  
   - Rebase merge: OFF（好み。rebase はブランチ側で実施する想定）

> ※ CI 導入後に **Require status checks to pass before merging** を ON にし、`frontend build` や `docker build` チェックを必須にするのが理想です。

## 設定手順（GUI）
1. GitHub → **Settings** → **Branches** → **Branch protection rules** → **Add rule**
2. **Branch name pattern** に `main`
3. 上記オプションをチェック
4. 保存

## 設定手順（gh CLI）
```bash
# 1 承認、会話解決必須、最新化必須、直 push 禁止（管理者除く）の例
gh api \
  -X PUT \
  repos/:owner/:repo/branches/main/protection \
  -f required_pull_request_reviews.dismiss_stale_reviews=true \
  -f required_pull_request_reviews.required_approving_review_count=1 \
  -f required_pull_request_reviews.require_code_owner_reviews=true \
  -f required_status_checks.strict=true \
  -f enforce_admins=true \
  -F restrictions='null' \
  -F required_status_checks='{"strict": true, "contexts": []}' \
  -F required_linear_history=true \
  -F allow_force_pushes=false \
  -F allow_deletions=false \
  -F block_creations=false
