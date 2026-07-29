package com.xiaopengqin.i18n

import java.io.File

internal class AndroidSourceScanner(private val marker: String) {
    private val xmlTextAttribute = Regex(
        """android:(?:text|hint|contentDescription|title|summary)\s*=\s*\"([^\"]+)\"""",
    )
    private val composeText = Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\"([^\"]+)\"""")
    private val quotedString = Regex("""\"([^\"\n]+)\"""")

    fun scan(file: File): List<I18nCandidate> {
        val content = file.readText()
        return when (file.extension.lowercase()) {
            "xml" -> scanXml(file, content)
            "kt", "java" -> scanCode(file, content)
            else -> emptyList()
        }
    }

    private fun scanXml(file: File, content: String): List<I18nCandidate> =
        xmlTextAttribute.findAll(content).mapNotNull { match ->
            candidate(file, content, match.range.first, "xml-attribute", match.groupValues[1], forcedOnly = false)
        }.toList()

    private fun scanCode(file: File, content: String): List<I18nCandidate> {
        val compose = composeText.findAll(content).mapNotNull { match ->
            candidate(file, content, match.range.first, "compose-text", match.groupValues[1], forcedOnly = false)
        }
        val forced = quotedString.findAll(content).mapNotNull { match ->
            val value = match.groupValues[1]
            if (!isMarked(value)) null else candidate(file, content, match.range.first, "marked-string", value, forcedOnly = true)
        }
        return (compose + forced).distinctBy { listOf(it.file, it.line, it.text) }.toList()
    }

    private fun candidate(
        file: File,
        content: String,
        offset: Int,
        kind: String,
        rawText: String,
        forcedOnly: Boolean,
    ): I18nCandidate? {
        val forced = isMarked(rawText)
        if (forcedOnly && !forced) return null
        val text = if (forced) rawText.removePrefix(marker).removeSuffix(marker) else rawText
        if (text.isBlank() || text.startsWith("@string/") || text.startsWith("${'$'}{")) return null
        val line = content.substring(0, offset).count { it == '\n' } + 1
        return I18nCandidate(file.path, line, kind, text, forced)
    }

    private fun isMarked(value: String): Boolean =
        marker.isNotEmpty() && value.length >= marker.length * 2 &&
            value.startsWith(marker) && value.endsWith(marker)
}
