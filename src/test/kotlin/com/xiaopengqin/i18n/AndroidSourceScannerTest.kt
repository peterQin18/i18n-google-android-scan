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

    @Test
    fun `scans base string resources with their Android resource names`() {
        val file = Files.createTempDirectory("res").resolve("values").toFile().also { it.mkdirs() }.resolve("strings.xml")
        file.writeText("""<resources><string name="app_name">My App</string><string name="internal" translatable="false">Internal</string></resources>""")

        assertEquals(
            listOf(I18nCandidate(file.path, 1, "string-resource", "My App", false, "app_name", "app_name")),
            AndroidSourceScanner("~").scan(file),
        )
    }

    @Test
    fun `uses composable context in generated keys`() {
        val file = Files.createTempFile("HomeScreen", ".kt").toFile()
        file.writeText("""@Composable fun HomeScreen() { Text("Load data") } @Composable fun DetailsScreen() { Text("Load data") }""")

        assertEquals(
            listOf("home_screen_load_data", "details_screen_load_data"),
            AndroidSourceScanner("~").scan(file).map(I18nCandidate::suggestedKey),
        )
    }

    @Test
    fun `uses a full-text hash for Chinese copy while preserving component context`() {
        val file = Files.createTempFile("HomeScreen", ".kt").toFile()
        file.writeText("""@Composable fun HomeScreen() { Text("加载 Fake 首页数据") }""")

        assertEquals("home_screen_06a079a712", AndroidSourceScanner("~").scan(file).single().suggestedKey)
    }

    @Test
    fun `uses the surrounding ordinary function as context for marked strings`() {
        val file = Files.createTempFile("HomeState", ".kt").toFile()
        file.writeText("""private fun HomeUiState.displayDescription(): String = "~正在请求首页数据~"""")

        val candidate = AndroidSourceScanner("~").scan(file).single()
        assertEquals("marked-string", candidate.kind)
        assertEquals("display_description_f02fc1a53b", candidate.suggestedKey)
    }
}
