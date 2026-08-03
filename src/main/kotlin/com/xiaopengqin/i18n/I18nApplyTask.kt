package com.xiaopengqin.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class I18nApplyTask : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val reportFile: RegularFileProperty
    @get:Input abstract val androidNamespace: Property<String>
    @get:OutputFile abstract val migrationFile: RegularFileProperty
    @get:org.gradle.api.tasks.Internal abstract val manifestFile: RegularFileProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun applyReplacements() {
        val entries = JsonReader(reportFile.get().asFile.readText()).read() as? List<*> ?: return
        val replacements = entries.mapNotNull { entry ->
            val candidate = entry as? Map<*, *> ?: return@mapNotNull null
            if (candidate["kind"] != "compose-text") return@mapNotNull null
            val path = candidate["file"] as? String ?: return@mapNotNull null
            val line = (candidate["line"] as? Number)?.toInt() ?: return@mapNotNull null
            val text = candidate["text"] as? String ?: return@mapNotNull null
            val key = candidate["suggestedKey"] as? String ?: return@mapNotNull null
            File(path) to ComposeReplacement(line, text, key)
        }.groupBy({ it.first }, { it.second })

        val changed = replacements.count { (file, values) ->
            ComposeSourceRewriter.rewrite(file, values, androidNamespace.get())
        }
        writeMarkedStringMigration(entries)
        writeManifest(entries)
        logger.lifecycle(
            "i18nApply: updated $changed Compose source file(s). " +
                "Non-Compose migration report: ${migrationFile.get().asFile}",
        )
    }

    private fun writeManifest(entries: List<*>) {
        val existing = I18nManifest.read(manifestFile.get().asFile).associateBy(ManifestEntry::key).toMutableMap()
        entries.mapNotNull { entry ->
            val candidate = entry as? Map<*, *> ?: return@mapNotNull null
            if (candidate["kind"] != "compose-text") return@mapNotNull null
            val key = candidate["suggestedKey"] as? String ?: return@mapNotNull null
            val text = candidate["text"] as? String ?: return@mapNotNull null
            ManifestEntry(key, text)
        }.forEach { existing[it.key] = it }
        I18nManifest.write(manifestFile.get().asFile, existing.values)
    }

    private fun writeMarkedStringMigration(entries: List<*>) {
        val marked = entries.mapNotNull { entry ->
            val candidate = entry as? Map<*, *> ?: return@mapNotNull null
            if (candidate["kind"] != "marked-string") return@mapNotNull null
            val file = candidate["file"] as? String ?: return@mapNotNull null
            val line = (candidate["line"] as? Number)?.toInt() ?: return@mapNotNull null
            val text = candidate["text"] as? String ?: return@mapNotNull null
            val key = candidate["suggestedKey"] as? String ?: return@mapNotNull null
            listOf(file, line.toString(), text, key)
        }
        val output = migrationFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(buildString {
            appendLine("# Non-Compose i18n migration")
            appendLine()
            appendLine("These marked strings were translated but not rewritten automatically, because changing a normal function from String to @StringRes Int affects its callers.")
            appendLine()
            marked.forEach { (file, line, text, key) ->
                appendLine("- `$file:$line` — `$text`")
                appendLine("  - Use `R.string.$key` as the function's `@StringRes Int` return value, then call `stringResource(...)` in the Compose caller.")
            }
        })
    }
}
