package com.xiaopengqin.i18n

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeSourceRewriterTest {
    @Test
    fun `replaces only scanned Text literal and adds imports`() {
        val file = Files.createTempFile("HomeScreen", ".kt").also {
            it.writeText("""package sample.feature

import androidx.compose.material3.Text

fun screen() { Text(text = "加载数据"); Text("Do not replace") }
""")
        }.toFile()

        assertTrue(
            ComposeSourceRewriter.rewrite(
                file,
                listOf(ComposeReplacement(5, "加载数据", "home_screen_123")),
                "sample.app",
            ),
        )
        assertEquals(
            """package sample.feature

import sample.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text

fun screen() { Text(text = stringResource(R.string.home_screen_123)); Text("Do not replace") }
""",
            file.readText(),
        )
    }
}
