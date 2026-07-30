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

# 中文说明

## 当前能力

- 扫描配置目录下的 Android XML、Kotlin 与 Java 文件。
- 识别待国际化的文本，以及显式标记的 `~文本~`。
- 将候选项输出为 `build/reports/i18n/scan.json`。
- 提供可在 CI 中执行的 `i18nCheck` 校验任务。

## 安装

当 `0.1.0` 通过 Gradle Plugin Portal 审核后，在需要扫描的 Android 模块中添加：

```kotlin
plugins {
    id("io.github.peterqin18.i18n-google-android-scan") version "0.1.0"
}
```

## 配置

```kotlin
androidI18n {
    sourceLocale.set("zh-CN")                 // 默认：zh-CN
    targetLocales.set(listOf("en", "zh-TW")) // 默认：en、zh-TW
    scanRoots.set(listOf("src/main"))         // 默认：src/main
    sourceMarker.set("~")                     // ~文本~ 强制作为候选项
    failOnCandidates.set(false)                // CI 中建议设为 true
}
```

多模块项目中，哪个 Android 模块需要扫描，就在哪个模块应用插件。

## 任务

```bash
# 扫描源码并生成报告
./gradlew :app:i18nScan

# 校验最近一次扫描报告
./gradlew :app:i18nCheck
```

严格 CI 可配置：

```kotlin
androidI18n {
    failOnCandidates.set(true)
}
```

随后执行 `./gradlew :app:i18nScan :app:i18nCheck`。

## 计划中的能力

Google Sheets 同步、术语表校验、LLM 翻译、自动生成字符串资源和源码替换均仍在计划中，**尚未在 0.1.0 中实现**。`i18nSync` 当前只是配置骨架，请不要用于生产同步。
