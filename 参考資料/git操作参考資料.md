
---

# 0) 前提

* 以降、メインブランチ名を `main` とします（`master` なら読み替えてください）。
* フロントエンド修正は例として `frontend/` 配下を触る前提で書きます（実プロジェクトのパスに合わせてください）。
* 自分の GitHub に **Fork** せず、直接このリポに push できる権限があるかで手順が少し変わります。以下は **共通**→（A: 直 push 可能）/（B: Fork 運用）両方書きます。

---

# 1) 初回セットアップ（共通）

```bash
# リポジトリを取得
git clone https://github.com/h-i500/spring-deposit-auth.git
cd spring-deposit-auth

# 取得した直後の状態を確認
git status
git remote -v
```

* 他メンバーが多ければ、**rebase 前提 pull** をデフォルトにすると履歴が綺麗です：

```bash
git config pull.rebase true
```

---

# 2) ブランチ戦略（命名ルール）

* 基本：`main` は常にデプロイ可能な状態に保つ。
* 作業は **トピックブランチ**（＝機能/修正ごと）で行う。
* 命名例

  * 機能追加: `feat/xxx`
  * バグ修正: `fix/yyy`
  * リファクタ: `refactor/zzz`
  * UI だけの小修正: `chore/ui-...`

例）

```bash
git checkout -b feat/login-page-error-toast
```

---

# 3) 実装 → ローカルコミット

```bash
# 変更
vim frontend/src/...   # 好きなエディタで編集

# 変更確認
git status
git add frontend/               # or ファイルを個別に add
git commit -m "feat(frontend): ログイン失敗時にエラートーストを表示"
```

* コミットメッセージは「型(範囲): 要約」の形式にすると履歴が読みやすいです。例：

  * `feat(frontend): ...` / `fix(frontend): ...` / `refactor(frontend): ...`

---

# 4) リモートへ push

### A) 直接 push できる場合

```bash
git push -u origin feat/login-page-error-toast
```

### B) Fork 運用の場合

1. GitHub 上で元リポを **Fork**
2. 自分の Fork を clone するか、または既に clone 済みなら upstream を登録

```bash
# まだなら自分のForkを origin として clone
git clone https://github.com/<your-account>/spring-deposit-auth.git
cd spring-deposit-auth

# 元(本家)リポを upstream として追加
git remote add upstream https://github.com/h-i500/spring-deposit-auth.git
git remote -v
```

3. 作業ブランチを自分の Fork に push

```bash
git push -u origin feat/login-page-error-toast
```

---

# 5) Pull Request を作成

* GitHub で `feat/...` → `main` への PR を作成。
* PR テンプレがなければ、**目的 / 変更点 / 動作確認手順 / スクショ**（UIなら）を添えるとレビューが早いです。

---

# 6) レビュー対応＆追従（main の更新を取り込む）

PR が開いている間に `main` が進むことはよくあります。自分のブランチへ **rebase** して履歴を綺麗に保ちます。

### A) 直 push 運用

```bash
# 最新 main を取得
git fetch origin
git checkout feat/login-page-error-toast
git rebase origin/main

# 競合が出たら解消 -> 続行
# (競合解消ファイルを編集して)
git add <解消したファイル>
git rebase --continue

# rebase 後は push -f が必要（履歴を書き換えたため）
git push -f
```

### B) Fork 運用

```bash
# 本家の更新を取り込む
git fetch upstream
git checkout feat/login-page-error-toast
git rebase upstream/main

# 競合解消して続行
git add <解消したファイル>
git rebase --continue

# 自分の Fork に強制 push
git push -f origin feat/login-page-error-toast
```

> ※ `rebase` が苦手なら `merge` でも可：
> `git merge origin/main`（or `upstream/main`）。
> ただし履歴がマージコミットで増えるので、チーム方針に合わせてください。

---

# 7) マージ（PR の取り込み）

* チーム方針に合わせて：

  * **Squash and merge**（小コミットを 1 つに潰す → 履歴が綺麗）
  * **Rebase and merge**（直線的な履歴に）
  * **Merge commit**（個々のコミットを残す）

UI/小修正なら **Squash** が無難です。

---

# 8) 競合が起きたときの実務フロー（詳解）

### 8.1 rebase 中に競合した

```bash
# 競合しているファイルを確認
git status

# エディタで <<<<<<< と >>>>>>> を解消
# どちらの変更を採るか/統合するか判断

# 解消したら
git add <ファイル>
git rebase --continue

# 中断したいとき
git rebase --abort
```

### 8.2 PR 上で GitHub が「Resolve conflicts」を求める

* 小さな競合なら GitHub の UI で解消 → Commit。
* 複雑ならローカルで 8.1 の手順を実施し、`push -f`。

### 8.3 すでに push 済みの自分のコミット同士が競合する/汚れた

* コミットを整理してから再 push：

```bash
# 直近数コミットをまとめて編集
git rebase -i HEAD~5
# pick / squash / reword を調整して保存

# 綺麗にしたら強制 push
git push -f
```

---

# 9) よくある便利コマンド

```bash
# 作業ブランチを最新mainから作り直したい（まだ共有してないなら安全）
git checkout main
git pull --rebase
git checkout -b feat/new-branch

# 変更の差分を確認
git diff               # 未ステージの差分
git diff --staged      # ステージ済みの差分

# 直前のコミットメッセージ修正（未 push 推奨）
git commit --amend

# 一時退避（WIPのまま別作業へ）
git stash
git stash pop

# 特定ファイルだけ直前のコミットから取り消し
git checkout -- frontend/src/App.tsx

# 特定のコミットを取り込む（cherry-pick）
git cherry-pick <commit-hash>
```

---

# 10) ローカル検証（フロントエンド）

* UI 変更は **スクショ** や **動作確認手順**を PR に載せるとレビュワーが確認しやすいです。
* 例：

```bash
# パッケージ導入/更新
npm ci   # または pnpm i / yarn

# ローカル起動
npm run dev   # または start
npm run test  # ユニットテストがあれば
npm run lint  # Lint を通してからコミット
```

---

# 11) 運用の小ネタ（チームで決めると良いこと）

* **ブランチ保護**：`main` への直 push 禁止、PR レビュー必須、CI 必須。
* **コミット規約**：Conventional Commits 風にして自動リリースノートに活用。
* **PR サイズ**：1 PR は小さく（\~200 行目安）、レビューを速く。
* **レビュー前チェック**：`lint`, `format`, `test` を CI で強制。

---

## まとめ：最短コース（コピペ用）

```bash
# 1) 最新 main を取得
git checkout main
git pull --rebase

# 2) 作業ブランチ作成
git checkout -b feat/login-page-error-toast

# 3) 変更してコミット
git add frontend/
git commit -m "feat(frontend): ログイン失敗時にエラートーストを表示"

# 4) push
git push -u origin feat/login-page-error-toast

# 5) GitHub で PR 作成（main へ）
# 6) main が進んだら取り込む（直 push運用）
git fetch origin
git rebase origin/main
git push -f

# 7) レビューが通ったら Squash and merge
```

---

必要であれば、このプロジェクト構成（パッケージマネージャ・ビルド/起動方法・CI 有無）に合わせて、**専用の PR テンプレート**や **ブランチ保護設定の案**まで作ります。リポジトリの現状ルール（ブランチ名、CI、main / master どっち？など）が分かれば、その前提で最適化版もお渡しできます。
