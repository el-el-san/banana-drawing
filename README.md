# Banana Drawing

🍌 An Android application that generates AI-powered illustrations of bananas and cats

## Overview

Banana Drawing is an Android app that combines images and text prompts to generate creative images using the Google Gemini API.

## Features

### 📱 bdrow-android-client
Native Android application built with Kotlin/Jetpack Compose.

**Key Features:**
- 📷 Select images from gallery
- ✏️ Image editing with red pen (drawing & eraser tools)
- 💬 Text prompt input
- 🤖 Image generation using Gemini API
- 💾 Save generated images
- 🎨 Aspect ratio-preserving image display

**Tech Stack:**
- Kotlin + Jetpack Compose
- Material Design 3
- MVVM Architecture
- Gemini API Integration
- GitHub Actions CI/CD

For details, see [bdrow-android-client/README.md](./bdrow-android-client/README.md).

## Build

### Android APK Build

Automated builds are configured with GitHub Actions:

```bash
# Push a tag to create release build
git tag v1.0.0
git push origin v1.0.0
```

Built APKs are available for download from GitHub Actions Artifacts and automatically published as GitHub Releases when tags are pushed.

### Local Build

For development with Android Studio:
```bash
cd bdrow-android-client
./gradlew assembleDebug
```

## CI/CD

GitHub Actions workflows automate the following processes:

1. **Android APK Build** - Creates Debug/Release builds
2. **Automated Testing** - Runs unit tests
3. **Release Creation** - Automatically creates GitHub releases when tags are pushed
4. **Version Management** - Auto-increments version based on commit messages

## Development Requirements

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34
- Kotlin 1.9.0 or later

## Setup

1. Clone the repository
```bash
git clone https://github.com/el-el-san/bdrow.git
cd bdrow
```

2. Get Gemini API Key
   - Obtain API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Enter the API key in the app's settings screen

3. Open the project in Android Studio

## License

MIT License - See [LICENSE](./LICENSE) for details.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
