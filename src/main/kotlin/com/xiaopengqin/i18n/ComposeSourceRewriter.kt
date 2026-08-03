package com.xiaopengqin.i18n

import java.io.File

internal data class ComposeReplacement(val line: Int, val text: String, val key: String)

internal object ComposeSourceRewriter {
    private val composeText = Regex("""\bText\s*\(\s*(?:text\s*=\s*)?(\"[^\"\n]*\")""")

    fun rewrite(file: File, replacements: List<ComposeReplacement>, androidNamespace: String): Boolean {
        var content = file.readText()
        val candidates = replacements.associateBy { it.line to it.text }
        val edits = composeText.findAll(content).mapNotNull { match ->
            val literal = match.groups[1] ?: return@mapNotNull null
            val text = literal.value.removeSurrounding("\"")
            val line = content.substring(0, literal.range.first).count { it == '\n' } + 1
            candidates[line to text]?.let { literal.range to "stringResource(R.string.${it.key})" }
        }.toList()
        if (edits.isEmpty()) return false
        edits.asReversed().forEach { (range, replacement) ->
            content = content.replaceRange(range, replacement)
        }
        content = addImport(content, "androidx.compose.ui.res.stringResource")
        val packageName = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE).find(content)?.groupValues?.get(1)
        if (packageName != null && packageName != androidNamespace && androidNamespace.isNotBlank()) {
            content = addImport(content, "$androidNamespace.R")
        }
        file.writeText(content)
        return true
    }

    private fun addImport(source: String, importName: String): String {
        if (Regex("""^import\s+$importName(?:\s|$)""", RegexOption.MULTILINE).containsMatchIn(source)) return source
        val packageMatch = Regex("""^package\s+[\w.]+\s*$""", RegexOption.MULTILINE).find(source) ?: return source
        return source.replaceRange(packageMatch.range.last + 1, packageMatch.range.last + 1, "\nimport $importName")
    }
}
