package com.xiaopengqin.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QwenTranslationParserTest {
    @Test
    fun `parses escaped native DashScope content and JSON translations`() {
        val body = """{"output":{"choices":[{"message":{"content":[{"text":"{\"translations\":[{\"key\":\"i18n_login\",\"locale\":\"en\",\"text\":\"Log in\"},{\"key\":\"i18n_login\",\"locale\":\"zh-TW\",\"text\":\"登入\"}]}"}]}}]}}"""

        assertEquals(
            listOf(
                ParsedTranslation("i18n_login", "en", "Log in"),
                ParsedTranslation("i18n_login", "zh-TW", "登入"),
            ),
            QwenTranslationParser.parse(body),
        )
    }

    @Test
    fun `supports Unicode and newline escapes`() {
        assertEquals("Hello\n中", QwenTranslationParser.decodeJsonString("Hello\\n\\u4e2d"))
    }

    @Test
    fun `rejects unstructured model output`() {
        val body = """{"output":{"choices":[{"message":{"content":[{"text":"translated text"}]}}]}}"""
        assertFailsWith<IllegalArgumentException> { QwenTranslationParser.parse(body) }
    }
}
