package com.xiaopengqin.i18n

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty
import javax.inject.Inject

abstract class AndroidI18nExtension @Inject constructor(objects: ObjectFactory) {
    val sourceLocale: Property<String> = objects.property(String::class.java).convention("zh-CN")
    val targetLocales: ListProperty<String> =
        objects.listProperty(String::class.java).convention(listOf("en", "zh-TW"))
    val scanRoots: ListProperty<String> =
        objects.listProperty(String::class.java).convention(listOf("src/main"))
    val generatedFileName: Property<String> =
        objects.property(String::class.java).convention("i18n_generated.xml")
    val outputDirectory: DirectoryProperty = objects.directoryProperty()
    val sourceMarker: Property<String> = objects.property(String::class.java).convention("~")
    val failOnCandidates: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val googleSheet: GoogleSheetConfig = objects.newInstance(GoogleSheetConfig::class.java)
    val glossary: GlossaryConfig = objects.newInstance(GlossaryConfig::class.java)
    val llm: LlmConfig = objects.newInstance(LlmConfig::class.java)

    fun googleSheet(action: Action<GoogleSheetConfig>) = action.execute(googleSheet)
    fun glossary(action: Action<GlossaryConfig>) = action.execute(glossary)
    fun llm(action: Action<LlmConfig>) = action.execute(llm)
}

abstract class GoogleSheetConfig @Inject constructor(objects: ObjectFactory) {
    val spreadsheetId: Property<String> = objects.property(String::class.java)
    val sheetName: Property<String> = objects.property(String::class.java).convention("android_translations")
    val credentialFile: RegularFileProperty = objects.fileProperty()
}

abstract class GlossaryConfig @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val spreadsheetId: Property<String> = objects.property(String::class.java)
    val sheetName: Property<String> = objects.property(String::class.java).convention("glossary")
    val failOnMissingTargetTranslation: Property<Boolean> =
        objects.property(Boolean::class.java).convention(true)
}

abstract class LlmConfig @Inject constructor(objects: ObjectFactory) {
    val baseUrl: Property<String> = objects.property(String::class.java)
    val apiKey: Property<String> = objects.property(String::class.java)
    val model: Property<String> = objects.property(String::class.java)
    val promptFile: RegularFileProperty = objects.fileProperty()
    val temperature: Property<Double> = objects.property(Double::class.java).convention(0.2)
}
