# キーストア設定手順

## 固定キーストアを使用する理由
毎回同じ署名でAPKを生成することで、アプリの更新インストールが可能になります。
異なる署名のAPKはパッケージ競合エラーでインストールできません。

## セットアップ手順

### 方法1: GitHub Secrets を使用（推奨）

1. **Base64エンコードされたキーストアを取得**
   ```bash
   cat .github/keystore/debug.keystore.base64
   ```
   または新規生成する場合：
   ```bash
   base64 -w 0 .github/keystore/debug.keystore
   ```

2. **GitHubリポジトリの設定**
   - リポジトリの Settings → Secrets and variables → Actions
   - 「New repository secret」をクリック
   - Name: `KEYSTORE_BASE64`
   - Value: 上記でコピーしたBase64文字列を貼り付け
   - 「Add secret」をクリック

3. **確認**
   - 次回のプッシュから固定キーストアが使用されます
   - ビルドログで「Using fixed keystore from GitHub Secrets」と表示されることを確認

### 方法2: リポジトリ内のキーストアを使用

1. **キーストアファイルが存在することを確認**
   ```bash
   ls -la .github/keystore/debug.keystore
   ```

2. **GitHub Secretsが設定されていない場合**
   - 自動的にリポジトリ内のキーストアが使用されます
   - ビルドログで「Using repository keystore file as fallback」と表示されます

## キーストア情報

- **Alias**: androiddebugkey
- **Store Password**: android
- **Key Password**: android
- **Validity**: 10000 days
- **Algorithm**: RSA 2048

## トラブルシューティング

### パッケージ競合エラーが発生する場合

1. **既存アプリを完全にアンインストール**
   ```bash
   adb uninstall com.example.bdrowclient
   ```

2. **設定から手動でアンインストール**
   - 設定 → アプリ → Bdrow Client → アンインストール

3. **新しいAPKをインストール**
   - GitHub Actionsから最新のAPKをダウンロード
   - インストール

### キーストアの再生成が必要な場合

```bash
# 新しいキーストアを生成
keytool -genkey -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Your Name,O=Your Org,C=JP"

# Base64エンコード
base64 -w 0 debug.keystore > debug.keystore.base64
```

## セキュリティ注意事項

- プロダクション環境では別の署名キーを使用してください
- debug.keystoreはデバッグビルド専用です
- Secretsに設定した値は他の人には見えません
- フォークされたリポジトリではSecretsは引き継がれません
