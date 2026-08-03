package com.xiaopengqin.i18n

data class I18nCandidate(
    val file: String,
    val line: Int,
    val kind: String,
    val text: String,
    val forced: Boolean,
    /** Existing @string name when the candidate came from res/values. */
    val resourceName: String? = null,
    /** Stable Android resource name proposed for a scanned source literal. */
    val suggestedKey: String? = null,
)
