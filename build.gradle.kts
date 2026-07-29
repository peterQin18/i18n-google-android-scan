plugins {
    id("com.gradle.plugin-publish") version "2.1.1"
    kotlin("jvm") version "2.1.10"
}

group = "io.github.peterqin18"
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("androidI18n") {
            id = "io.github.peterqin18.i18n-google-android-scan"
            implementationClass = "com.xiaopengqin.i18n.AndroidI18nPlugin"
            displayName = "Android Google Sheets i18n scanner"
            description = "Scans Android hardcoded copy and synchronizes resource translations."
            tags.set(listOf("android", "i18n", "localization", "google-sheets"))
            website.set("https://github.com/peterQin18/i18n-google-android-scan")
            vcsUrl.set("https://github.com/peterQin18/i18n-google-android-scan.git")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
