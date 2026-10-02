# Contributing to Reflex

Thank you for your interest in contributing to Reflex! We welcome contributions that align with our commitment to privacy, calm local-first utility, and thoughtful tactile design.

---

## Development Environment Setup

### Required Tools
- **JDK**: Java 17 or higher
- **Android SDK**: API 35 (Android 15) build tools
- **Android Studio**: Ladybug (2024.2.1) / Meerkat or newer
- **Gradle Wrapper**: 8.13 (bundled with repository)

### Getting the Code
```bash
git clone https://github.com/bobby-99/reflex.git
cd reflex
./gradlew assembleDebug
```

---

## Contribution Guidelines & Principles

Before submitting code, please ensure your changes adhere to our core project tenets:

### 1. Strict Offline & Privacy Guarantee
- **No Network Code**: Never add `android.permission.INTERNET` or any network call.
- **No Analytics / Telemetry**: No tracking SDKs, no cloud sync, and no third-party diagnostics.
- **Local Sovereignty**: All user data must stay on the device in Room / DataStore.

### 2. Design System v1.0
- Reflex uses a custom design language defined in [DESIGN.md](DESIGN.md).
- Typography uses **Lora** font family.
- Color palettes and theme tokens are defined in `app/src/main/java/com/reflex/app/ui/theme/Tokens.kt`.
- Use `ReflexCard`, `ReflexTopBar`, `ReflexBottomNavBar`, and tactile haptics where applicable.

### 3. Clean Code & Logging
- Do not use `println` or `Throwable.printStackTrace()`. Use `com.reflex.app.util.AppLog` with appropriate log levels.
- Wrap non-essential operations in descriptive try/catch blocks with logging.
- Maintain existing comments, architecture patterns (MVVM + unidirectional StateFlow), and clean file separation.

---

## Pull Request Process

1. **Fork the repository** and create a descriptive branch:
   ```bash
   git checkout -b feature/my-cool-improvement
   ```
2. **Implement your change** and make sure unit tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. **Verify the builds**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
4. **Submit a Pull Request** against the `main` branch with a clear description of the problem solved and testing steps performed.

---

## Code of Conduct

All contributors and participants must adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).
