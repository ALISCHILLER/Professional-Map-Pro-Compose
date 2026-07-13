package com.msa.professionalmap.core.offline.data

internal class OfflineDownloadMessages private constructor(
    private val languageTag: String,
) {
    fun notificationTitle(): String =
        if (isPersian) "در حال دانلود نقشه آفلاین" else "Downloading offline map"

    fun notificationBody(title: String, percent: Int): String =
        if (isPersian) {
            "$title · ${percent.coerceIn(0, 100).toString().localizedDigits()}٪"
        } else {
            "$title · ${percent.coerceIn(0, 100)}%"
        }

    fun queued(): String = if (isPersian) "در صف دانلود" else "Queued"

    fun progress(percent: Int): String =
        if (isPersian) {
            "در حال دانلود ${percent.coerceIn(0, 100).toString().localizedDigits()}٪"
        } else {
            "Downloading ${percent.coerceIn(0, 100)}%"
        }

    fun ready(): String = if (isPersian) "نقشه آفلاین آماده است." else "Offline region ready."

    private val isPersian: Boolean get() = languageTag.startsWith(PersianLanguagePrefix)

    private fun String.localizedDigits(): String = buildString(length) {
        for (char in this@localizedDigits) {
            append(
                when (char) {
                    '0' -> '۰'
                    '1' -> '۱'
                    '2' -> '۲'
                    '3' -> '۳'
                    '4' -> '۴'
                    '5' -> '۵'
                    '6' -> '۶'
                    '7' -> '۷'
                    '8' -> '۸'
                    '9' -> '۹'
                    else -> char
                }
            )
        }
    }

    companion object {
        const val DefaultLanguageTag = "en"
        private const val PersianLanguagePrefix = "fa"

        fun from(languageTag: String?): OfflineDownloadMessages = OfflineDownloadMessages(
            languageTag = languageTag.orEmpty().ifBlank { DefaultLanguageTag }.lowercase(),
        )
    }
}
