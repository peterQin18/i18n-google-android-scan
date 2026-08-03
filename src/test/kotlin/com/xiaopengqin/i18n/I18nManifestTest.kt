package com.xiaopengqin.i18n

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class I18nManifestTest {
    @Test
    fun `persists applied source keys and text`() {
        val file = Files.createTempDirectory("i18n").resolve("i18n-manifest.json").toFile()
        val entries = listOf(ManifestEntry("home_screen_load", "加载数据"))

        I18nManifest.write(file, entries)

        assertEquals(entries, I18nManifest.read(file))
    }
}
