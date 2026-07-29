package com.xiaopengqin.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class I18nScanTask : DefaultTask() {
    @get:InputDirectory
    abstract val projectDirectory: DirectoryProperty

    @get:Input
    abstract val scanRoots: ListProperty<String>

    @get:Input
    abstract val sourceMarker: Property<String>

    @TaskAction
    fun scan() {
        val scanner = AndroidSourceScanner(sourceMarker.get())
        val root = projectDirectory.get().asFile
        val candidates = scanRoots.get()
            .flatMap { relativeRoot -> root.resolve(relativeRoot).walkTopDown().toList() }
            .filter { it.isFile && it.extension.lowercase() in setOf("xml", "kt", "java") }
            .flatMap(scanner::scan)

        val report = project.layout.buildDirectory.file("reports/i18n/scan.json").get().asFile
        report.parentFile.mkdirs()
        report.writeText(candidates.toJson())
        logger.lifecycle("i18nScan: found ${candidates.size} candidate(s). Report: ${report.relativeTo(root)}")
    }
}

abstract class I18nCheckTask : DefaultTask() {
    @get:Input
    abstract val failOnCandidates: Property<Boolean>

    @TaskAction
    fun check() {
        val report = project.layout.buildDirectory.file("reports/i18n/scan.json").get().asFile
        if (!report.exists()) {
            throw GradleException("Run i18nScan before i18nCheck.")
        }
        if (failOnCandidates.get() && report.readText().contains("\\\"text\\\"")) {
            throw GradleException("i18nCheck failed: hardcoded i18n candidates remain. See ${report.path}")
        }
        logger.lifecycle("i18nCheck: passed.")
    }
}

abstract class I18nSyncTask : DefaultTask() {
    @get:Input
    abstract val spreadsheetId: Property<String>

    @get:Input
    abstract val glossaryEnabled: Property<Boolean>

    @TaskAction
    fun sync() {
        require(spreadsheetId.isPresent) { "androidI18n.googleSheet.spreadsheetId is required for i18nSync." }
        logger.lifecycle(
            "i18nSync scaffold is configured for spreadsheet ${spreadsheetId.get()}. " +
                "Google Sheets pull/push and LLM translation are the next implementation slice.",
        )
        if (glossaryEnabled.get()) {
            logger.lifecycle("i18nSync will apply the remote glossary before every LLM translation.")
        }
    }
}

private fun List<I18nCandidate>.toJson(): String = joinToString(
    prefix = "[\n",
    postfix = "\n]\n",
    separator = ",\n",
) { candidate ->
    "  {\"file\":\"${candidate.file.escapeJson()}\",\"line\":${candidate.line},\"kind\":\"${candidate.kind}\",\"text\":\"${candidate.text.escapeJson()}\",\"forced\":${candidate.forced}}"
}

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
