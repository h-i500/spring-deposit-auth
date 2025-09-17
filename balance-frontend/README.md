

## 起動
```bash
$ pnpm install
$ pnpm dev
```

## 401エラーへの対処
残高照会で401エラーが出る場合は、
プロジェクトトップで以下を実行し kc.pub を生成します。
```
$ scripts/key-create.sh
```
kc.pubの内容を kong/kong.yml へ書き込み、以下でkongをrestartする。

```
$ docker compose restart kong
```
