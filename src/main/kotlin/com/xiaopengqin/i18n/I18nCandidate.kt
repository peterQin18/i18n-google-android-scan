package com.xiaopengqin.i18n

data class I18nCandidate(
    val file: String,
    val line: Int,
    val kind: String,
    val text: String,
    val forced: Boolean,
)

