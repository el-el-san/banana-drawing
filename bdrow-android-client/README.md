# Bdrow Android Client (Gemini)

Jetpack Composeで実装したAndroidネイティブアプリ。Google Gemini APIと連携し、画像とテキストの組み合わせから画像生成・説明などを行います。Rust/JNI 連携は行っていません（Geminiベースに刷新）。

## 機能

- 画像の選択（最大4枚）とプレビューグリッド表示（Bentoレイアウト）
- 画像への手描き注釈（ペン/消しゴム、ブラシサイズ調整）
- テキストプロンプト入力と送信
- Gemini APIへのリクエスト（画像+テキスト、テキストのみ、画像生成）
- 結果テキスト/画像の表示、画像保存
- APIキーの暗号化保存（EncryptedSharedPreferences）

## アーキテクチャ概要

```
Android (Kotlin / Jetpack Compose)
  ├─ UI: SingleScreen + モダンUIコンポーネント
  ├─ VM: AndroidViewModel(MainViewModel)
  └─ Data: GeminiRepository + Retrofit/Google AI SDK
```

- `MainViewModel`: 画面状態管理、送信処理の入口
- `GeminiRepository`: 画像のBase64化、二段階プロンプト（翻訳→生成）、RetrofitでのREST呼び出し/Google AI SDKの併用
- `ApiKeyPreferences`: APIキーを端末内で暗号化保存

## 必要要件

- Android 7.0 (API 24) 以上
- JDK 17 / Android SDK 34

## セットアップ（APIキー）

1. [Google AI Studio](https://aistudio.google.com/)でGemini APIキーを取得
2. アプリ内のAPIキー設定ダイアログから入力・保存

## ビルド（GitHub Actions）

このリポジトリには以下のワークフローが含まれます：

- `.github/workflows/build-android.yml`: Debug/Release APKのビルドとArtifactsのアップロード
- `.github/workflows/auto-version.yml`: コミット内容に応じた`version.properties`の自動更新

実行方法：
- ブランチへプッシュ/PRで自動実行（main/develop など）
- もしくは Actions タブで「Build Android App」を手動実行（workflow_dispatch）

署名キーストア：
- `KEYSTORE_BASE64`シークレットが設定されていればそれを使用
- 未設定の場合はワークフロー内でデバッグ用キーストアを自動生成

## ローカルビルド（任意）

```bash
cd bdrow-android-client
./gradlew assembleDebug
```

## 主なディレクトリ

```
bdrow-android-client/
├── app/
│   ├── src/main/java/com/example/bdrowclient/
│   │   ├── ui/…（SingleScreen, components,…）
│   │   ├── data/…（api, models, repository, preferences）
│   │   └── MainActivity.kt / MainViewModel.kt
│   └── src/main/res/…
└── .github/workflows/…
```

## ライセンス

MIT License（リポジトリ直下の `LICENSE` を参照）
- ストレージ容量を確認

## 📝 ライセンス

Apache License 2.0 - 詳細は [LICENSE](../LICENSE) を参照

## 🤝 コントリビューション

1. Fork it
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📮 サポート

- Issues: [GitHub Issues](https://github.com/yourusername/bdrow-android-client/issues)
- Discussions: [GitHub Discussions](https://github.com/yourusername/bdrow-android-client/discussions)

## 🙏 謝辞

- [OpenAI Codex](https://github.com/openai/codex) - オリジナル実装
- [codex-ERI](https://github.com/el-el-san/codex-ERI) - 拡張版実装
- Rust/Android コミュニティ

---

Made with ❤️ using Codex-ERI and Rust
