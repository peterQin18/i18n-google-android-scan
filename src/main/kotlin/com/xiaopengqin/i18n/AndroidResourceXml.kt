package com.xiaopengqin.i18n

import java.io.File

internal object AndroidResourceXml {
    private val stringTag = Regex(
        """<string\s+[^>]*\bname\s*=\s*"([A-Za-z0-9_]+)"[^>]*>(.*?)</string>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun read(file: File): LinkedHashMap<String, String> = linkedMapOf<String, String>().apply {
        if (file.isFile) stringTag.findAll(file.readText()).forEach { match ->
            put(match.groupValues[1], decode(match.groupValues[2]))
        }
    }

    fun write(file: File, strings: Map<String, String>) {
        file.parentFile.mkdirs()
        file.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            strings.entries.joinToString("\n") { "    <string name=\"${it.key}\">${encode(it.value)}</string>" } +
            "\n</resources>\n")
    }

    private fun decode(value: String): String = value.replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
    private fun encode(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "\\\"").replace("'", "\\'")
}
