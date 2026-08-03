package com.xiaopengqin.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest

abstract class I18nTranslateTask : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val reportFile: RegularFileProperty
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) abstract val projectDirectory: DirectoryProperty
    @get:Input abstract val sourceLocale: Property<String>
    @get:Input abstract val targetLocales: ListProperty<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @get:Input abstract val generatedFileName: Property<String>
    @get:Input abstract val baseUrl: Property<String>
    @get:Internal abstract val apiKey: Property<String>
    @get:Input abstract val model: Property<String>
    @get:Input abstract val temperature: Property<Double>

    @TaskAction fun translate() {
        require(apiKey.isPresent) { "Set DASHSCOPE_API_KEY before running i18nTranslate." }
        val candidates = readCandidates()
        if (candidates.isEmpty()) return
        val sourceStrings = linkedMapOf<String, String>()
        val resourceKeys = mutableSetOf<String>()
        candidates.forEach { (text, resourceName, suggestedKey) ->
            val key = suggestedKey ?: resourceName ?: stableKey(text)
            sourceStrings.putIfAbsent(key, text)
            if (resourceName != null) resourceKeys += key
        }
        val prompt = """
            Translate the Android UI strings below from ${sourceLocale.get()} to ${targetLocales.get().joinToString()}.
            Return exactly one JSON object and no Markdown:
            {"translations":[{"key":"the supplied key","locale":"one requested locale","text":"translated UI string"}]}
            Include one translation for every supplied key and requested locale. Preserve placeholders such as %1${'$'}s and {name}.
            Source strings: ${sourceStrings.entries.joinToString(" | ") { "${it.key}=${it.value}" }}
        """.trimIndent()
        val body = "{\"model\":\"${model.get()}\",\"input\":{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":${json(prompt)}}]}]}}"
        val response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(baseUrl.get())).header("Authorization", "Bearer ${apiKey.get()}").header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw GradleException(
                "Translation request failed: HTTP ${response.statusCode()}. " +
                    "Provider response: ${response.body().take(1_000)}",
            )
        }
        val parsedTranslations = try {
            QwenTranslationParser.parse(response.body())
        } catch (error: IllegalArgumentException) {
            throw GradleException("Could not parse the Qwen translation response: ${error.message}", error)
        }
        val translations = parsedTranslations.groupBy(ParsedTranslation::locale)
        val audit = reportFile.get().asFile.parentFile.resolve("translations.json")
        audit.writeText("{\n  \"rawResponse\":${json(response.body())},\n  \"parsedCount\":${parsedTranslations.size}\n}\n")
        outputDirectory.get().asFile.mkdirs()
        write("values", sourceStrings.filterKeys { it !in resourceKeys }, resourceKeys)
        targetLocales.get().forEach { locale ->
            val manuallyTranslated = existingResourceNames(locale)
            write(
                "values-" + locale.replace("zh-TW", "zh-rTW"),
                translations[locale].orEmpty()
                    .filter { it.key in sourceStrings && it.key !in manuallyTranslated }
                    .associate { it.key to it.text },
                manuallyTranslated,
            )
        }
    }
    private fun readCandidates(): List<Triple<String, String?, String?>> {
        val entries = JsonReader(reportFile.get().asFile.readText()).read() as? List<*>
            ?: throw GradleException("i18n scan report is not a JSON array.")
        return entries.mapNotNull { entry ->
            val candidate = entry as? Map<*, *> ?: return@mapNotNull null
            val text = candidate["text"] as? String ?: return@mapNotNull null
            Triple(text, candidate["resourceName"] as? String, candidate["suggestedKey"] as? String)
        }
    }
    private fun existingResourceNames(locale: String): Set<String> {
        val qualifier = "values-" + locale.replace("zh-TW", "zh-rTW")
        val directory = projectDirectory.get().asFile.resolve("src/main/res/$qualifier")
        if (!directory.isDirectory) return emptySet()
        val pattern = Regex("""<string\s+[^>]*\bname\s*=\s*[\"']([A-Za-z0-9_]+)[\"']""")
        return directory.walkTopDown().filter { it.isFile && it.extension == "xml" }
            .flatMap { pattern.findAll(it.readText()).map { match -> match.groupValues[1] } }.toSet()
    }
    private fun stableKey(text: String): String = "i18n_" + MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }.take(10)
    private fun write(dir: String, strings: Map<String, String>, removeKeys: Set<String> = emptySet()) {
        val file = outputDirectory.get().asFile.resolve("$dir/${generatedFileName.get()}")
        val merged = AndroidResourceXml.read(file).apply {
            keys.removeAll(removeKeys)
            putAll(strings)
        }
        AndroidResourceXml.write(file, merged)
    }
    private fun json(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
}
