package com.xiaopengqin.i18n

internal data class ParsedTranslation(
    val key: String,
    val locale: String,
    val text: String,
)

/** Parses the native DashScope response and the JSON contract requested from the model. */
internal object QwenTranslationParser {
    fun parse(responseBody: String): List<ParsedTranslation> {
        val response = JsonReader(responseBody).read() as? Map<*, *>
            ?: error("Qwen response is not a JSON object.")
        val content = response.objectValue("output")
            .arrayValue("choices").firstOrNull().asObject()
            .objectValue("message").arrayValue("content").firstOrNull().asObject()
            .stringValue("text")
        val translated = JsonReader(content.trim().removeCodeFence()).read() as? Map<*, *>
            ?: error("Qwen translation content is not a JSON object.")
        val items = translated.arrayValue("translations")
        return items.map {
            val item = it.asObject()
            ParsedTranslation(
                key = item.stringValue("key"),
                locale = item.stringValue("locale"),
                text = item.stringValue("text"),
            )
        }.also { require(it.isNotEmpty()) { "Qwen response contained no translations." } }
    }

    fun decodeJsonString(value: String): String = JsonReader("\"$value\"").read() as String

    private fun String.removeCodeFence(): String =
        removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private fun Any?.asObject(): Map<*, *> = this as? Map<*, *>
        ?: error("Expected a JSON object in Qwen response.")

    private fun Map<*, *>.objectValue(name: String): Map<*, *> = get(name).asObject()
    private fun Map<*, *>.arrayValue(name: String): List<*> = get(name) as? List<*>
        ?: error("Expected JSON array '$name' in Qwen response.")
    private fun Map<*, *>.stringValue(name: String): String = get(name) as? String
        ?: error("Expected JSON string '$name' in Qwen response.")
}

/** Minimal non-recursive JSON reader for HTTP payloads; avoids regex backtracking on LLM output. */
internal class JsonReader(private val input: String) {
    private var index = 0

    fun read(): Any? {
        val value = readValue()
        skipWhitespace()
        require(index == input.length) { "Unexpected content after JSON value." }
        return value
    }

    private fun readValue(): Any? {
        skipWhitespace()
        return when (peek()) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> readLiteral("true", true)
            'f' -> readLiteral("false", false)
            'n' -> readLiteral("null", null)
            else -> readNumber()
        }
    }

    private fun readObject(): Map<String, Any?> {
        expect('{')
        val values = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (consume('}')) return values
        while (true) {
            skipWhitespace()
            val name = readString()
            skipWhitespace(); expect(':')
            values[name] = readValue()
            skipWhitespace()
            if (consume('}')) return values
            expect(',')
        }
    }

    private fun readArray(): List<Any?> {
        expect('[')
        val values = mutableListOf<Any?>()
        skipWhitespace()
        if (consume(']')) return values
        while (true) {
            values += readValue()
            skipWhitespace()
            if (consume(']')) return values
            expect(',')
        }
    }

    private fun readString(): String {
        expect('"')
        return buildString {
            while (true) {
                require(index < input.length) { "Unterminated JSON string." }
                when (val character = input[index++]) {
                    '"' -> return@buildString
                    '\\' -> append(readEscape())
                    else -> append(character)
                }
            }
        }
    }

    private fun readEscape(): Char {
        require(index < input.length) { "Invalid JSON escape sequence." }
        return when (val escape = input[index++]) {
            '"', '\\', '/' -> escape
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                require(index + 4 <= input.length) { "Invalid unicode JSON escape sequence." }
                input.substring(index, index + 4).toInt(16).toChar().also { index += 4 }
            }
            else -> error("Unsupported JSON escape sequence: \\$escape")
        }
    }

    private fun readNumber(): Number {
        val start = index
        while (index < input.length && input[index] !in ",]} \t\r\n") index++
        return input.substring(start, index).toDoubleOrNull()
            ?: error("Invalid JSON token at offset $start.")
    }

    private fun <T> readLiteral(literal: String, value: T): T {
        require(input.regionMatches(index, literal, 0, literal.length)) { "Expected '$literal'." }
        index += literal.length
        return value
    }

    private fun peek(): Char {
        require(index < input.length) { "Unexpected end of JSON input." }
        return input[index]
    }
    private fun expect(character: Char) { require(consume(character)) { "Expected '$character' at offset $index." } }
    private fun consume(character: Char): Boolean = if (index < input.length && input[index] == character) { index++; true } else false
    private fun skipWhitespace() { while (index < input.length && input[index].isWhitespace()) index++ }
}
