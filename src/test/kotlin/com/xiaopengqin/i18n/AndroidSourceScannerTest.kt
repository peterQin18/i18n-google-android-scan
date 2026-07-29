package com.xiaopengqin.i18n

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidSourceScannerTest {
    @Test
    fun `scans Android XML text attributes and ignores string references`() {
        val file = Files.createTempFile("screen", ".xml")
        file.writeText(
            """
            <TextView android:text="登录" android:hint="@string/login_hint" />
            <ImageView android:contentDescription="关闭" />
            """.trimIndent(),
        )

        val candidates = AndroidSourceScanner("~").scan(file.toFile())

        assertEquals(listOf("登录", "关闭"), candidates.map(I18nCandidate::text))
        assertTrue(candidates.all { it.kind == "xml-attribute" })
    }

    @Test
    fun `scans Compose text and forced marker strings`() {
        val file = Files.createTempFile("Screen", ".kt")
        file.writeText(
            """
            Text("提交")
            val analyticsName = "~支付成功~"
            Text(stringResource(R.string.already_localized))
            """.trimIndent(),
        )

        val candidates = AndroidSourceScanner("~").scan(file.toFile())

        assertEquals(listOf("提交", "支付成功"), candidates.map(I18nCandidate::text))
        assertEquals(listOf(false, true), candidates.map(I18nCandidate::forced))
    }
}
