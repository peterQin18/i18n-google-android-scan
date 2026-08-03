package com.xiaopengqin.i18n

import java.io.File
import java.security.MessageDigest

internal class AndroidSourceScanner(private val marker: String) {
    private val xmlTextAttribute = Regex(
        """android:(?:text|hint|contentDescription|title|summary)\s*=\s*\"([^\"]+)\"""",
    )
    private val stringResource = Regex(
        """<string\s+[^>]*\bname\s*=\s*"([A-Za-z0-9_]+)"[^>]*>(.*?)</string>""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val composeText = Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\"([^\"]+)\"""")
    private val quotedString = Regex("""\"([^\"\n]+)\"""")
    private val composableFunction = Regex("""@Composable(?:\s|\([^)]*\))*fun\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val kotlinFunction = Regex("""\bfun\s+(?:[A-Za-z_][A-Za-z0-9_.<>?]*\.)?([A-Za-z_][A-Za-z0-9_]*)\s*\(""")

    fun scan(file: File): List<I18nCandidate> {
        val content = file.readText()
        return when (file.extension.lowercase()) {
            "xml" -> scanXml(file, content)
            "kt", "java" -> scanCode(file, content)
            else -> emptyList()
        }
    }

    private fun scanXml(file: File, content: String): List<I18nCandidate> {
        val attributes = xmlTextAttribute.findAll(content).mapNotNull { match ->
            candidate(file, content, match.range.first, "xml-attribute", match.groupValues[1], forcedOnly = false)
        }
        val resources = if (file.parentFile.name == "values") {
            stringResource.findAll(content).mapNotNull { match ->
                val tag = match.value
                if ("translatable=\"false\"" in tag || "translatable='false'" in tag) return@mapNotNull null
                candidate(file, content, match.range.first, "string-resource", match.groupValues[2], forcedOnly = false)
                    ?.copy(resourceName = match.groupValues[1], suggestedKey = match.groupValues[1])
            }
        } else emptySequence()
        return (attributes + resources).toList()
    }

    private fun scanCode(file: File, content: String): List<I18nCandidate> {
        val composables = composableFunction.findAll(content).map { it.range.first to it.groupValues[1] }.toList()
        val functions = kotlinFunction.findAll(content).map { it.range.first to it.groupValues[1] }.toList()
        val compose = composeText.findAll(content).mapNotNull { match ->
            candidate(file, content, match.range.first, "compose-text", match.groupValues[1], forcedOnly = false)
                ?.copy(suggestedKey = contextualKey(file, composables, match.range.first, match.groupValues[1]))
        }
        val forced = quotedString.findAll(content).mapNotNull { match ->
            val value = match.groupValues[1]
            if (!isMarked(value)) null else candidate(file, content, match.range.first, "marked-string", value, forcedOnly = true)
                ?.copy(suggestedKey = contextualKey(file, functions, match.range.first, value.removePrefix(marker).removeSuffix(marker)))
        }
        return (compose + forced).distinctBy { listOf(it.file, it.line, it.text, it.suggestedKey) }.toList()
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

    private fun contextualKey(
        file: File,
        composables: List<Pair<Int, String>>,
        offset: Int,
        text: String,
    ): String {
        val component = composables.lastOrNull { it.first < offset }?.second ?: file.nameWithoutExtension
        val prefix = component.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
        val words = text.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        // Do not derive a partial key from mixed Chinese/English copy (e.g. only "Fake").
        // The component prefix remains readable while the hash keeps the whole source text distinct.
        val suffix = words.takeIf { it.isNotBlank() && text.all { character -> character.code < 128 } }
            ?.take(36) ?: shortHash(text)
        return "${prefix}_${suffix}".replace(Regex("_+"), "_")
    }

    private fun shortHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(10)
}
