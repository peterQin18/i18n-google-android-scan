package com.xiaopengqin.i18n

import java.io.File

internal data class ManifestEntry(val key: String, val text: String)

/** Source-controlled history of literals that were replaced by i18nApply. */
internal object I18nManifest {
    fun read(file: File): List<ManifestEntry> {
        if (!file.isFile) return emptyList()
        val values = JsonReader(file.readText()).read() as? List<*> ?: return emptyList()
        return values.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val key = map["key"] as? String ?: return@mapNotNull null
            val text = map["text"] as? String ?: return@mapNotNull null
            ManifestEntry(key, text)
        }
    }

    fun write(file: File, entries: Collection<ManifestEntry>) {
        file.parentFile.mkdirs()
        file.writeText(entries.distinctBy(ManifestEntry::key).joinToString(
            prefix = "[\n", postfix = "\n]\n", separator = ",\n",
        ) { entry -> "  {\"key\":\"${entry.key.escapeJson()}\",\"text\":\"${entry.text.escapeJson()}\"}" })
    }
}

internal fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
