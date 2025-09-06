# Banana Drawing

🍌 バナナと猫のイラストをAIで生成するAndroidアプリケーション

## 概要

Banana Drawingは、画像とテキストプロンプトを組み合わせて、Google Gemini APIを使用してクリエイティブな画像を生成するAndroidアプリです。

## 主な機能

### 📱 bdrow-android-client
Androidネイティブアプリケーション。Kotlin/Jetpack Composeで実装されています。

**主な機能:**
- 📷 ギャラリーから画像を選択
- ✏️ 赤ペンによる画像編集（描画・消しゴム機能）
- 💬 テキストプロンプトの入力
- 🤖 Gemini APIによる画像生成
- 💾 生成画像の保存
- 🎨 アスペクト比を保持した画像表示

**技術スタック:**
- Kotlin + Jetpack Compose
- Material Design 3
- MVVM アーキテクチャ
- Gemini API統合
- GitHub Actions CI/CD

詳細は [bdrow-android-client/README.md](./bdrow-android-client/README.md) を参照してください。

## ビルド

### Android APKのビルド

GitHub Actionsで自動ビルドが設定されています：

```bash
# タグをプッシュしてリリースビルドを作成
git tag v1.0.0
git push origin v1.0.0
```

ビルドされたAPKは、GitHub ActionsのArtifactsからダウンロード可能です。

### ローカルビルド

Android Studioでの開発：
```bash
cd bdrow-android-client
./gradlew assembleDebug
```

## CI/CD

GitHub Actionsワークフローが以下の処理を自動化しています：

1. **Android APKのビルド** - Debug/Releaseビルドの作成
2. **自動テスト** - ユニットテストの実行
3. **リリース作成** - タグプッシュ時に自動でGitHubリリースを作成

## 開発環境要件

- Android Studio Arctic Fox以降
- JDK 17
- Android SDK 34
- Kotlin 1.9.0以降

## セットアップ

1. リポジトリをクローン
```bash
git clone https://github.com/el-el-san/bdrow.git
cd bdrow
```

2. Gemini APIキーを取得
   - [Google AI Studio](https://makersuite.google.com/app/apikey)でAPIキーを取得
   - アプリ内の設定画面でAPIキーを入力

3. Android Studioでプロジェクトを開く

## ライセンス

MIT License - 詳細は [LICENSE](./LICENSE) を参照してください。

## Contributing

プルリクエストを歓迎します。大きな変更の場合は、まずissueを開いて変更内容について議論してください。
