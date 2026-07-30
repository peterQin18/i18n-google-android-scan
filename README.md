# Android i18n Scanner Gradle Plugin

Scans Android XML, Kotlin, and Java source files for hard-coded text that should be moved to localized resources. It produces a machine-readable report and can fail CI when candidates remain.

## Current capabilities

- Scans XML, Kotlin, and Java files under configurable source roots.
- Detects Android text candidates and explicit marker text such as `~Sign in~`.
- Writes a JSON report to `build/reports/i18n/scan.json`.
- Provides an `i18nCheck` task for CI enforcement.

## Installation

After version `0.1.0` is accepted by the Gradle Plugin Portal, apply it in the Android module you want to scan:

```kotlin
plugins {
    id("io.github.peterqin18.i18n-google-android-scan") version "0.1.0"
}
```

## Configuration

```kotlin
androidI18n {
    sourceLocale.set("zh-CN")              // Default: zh-CN
    targetLocales.set(listOf("en", "zh-TW")) // Default: en, zh-TW
    scanRoots.set(listOf("src/main"))      // Default: src/main
    sourceMarker.set("~")                  // ~Text~ forces a candidate
    failOnCandidates.set(false)             // Set true in CI
}
```

For a multi-module build, apply the plugin in each Android module whose source should be scanned.

## Tasks

```bash
# Scan sources and write build/reports/i18n/scan.json
./gradlew :app:i18nScan

# Validate the most recent report
./gradlew :app:i18nCheck
```

A strict CI configuration can use:

```kotlin
androidI18n {
    failOnCandidates.set(true)
}
```

Then run `./gradlew :app:i18nScan :app:i18nCheck`.

## Planned capabilities

Google Sheets synchronization, glossary enforcement, LLM translation, generated string resources, and automatic source replacement are planned but **not implemented in version 0.1.0**. The `i18nSync` task is currently a configuration scaffold only; do not use it for production synchronization.

## License

MIT
