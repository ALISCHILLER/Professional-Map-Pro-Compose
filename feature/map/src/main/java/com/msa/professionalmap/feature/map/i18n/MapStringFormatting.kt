package com.msa.professionalmap.feature.map.i18n

import java.util.Locale

internal fun Double.formatOne(): String = String.format(Locale.US, "%.1f", this)

internal fun Double.formatZero(): String = String.format(Locale.US, "%.0f", this)

internal fun Double.formatCoordinate(): String = String.format(Locale.US, "%.5f", this)

internal fun String.localizedDigits(): String = buildString {
    this@localizedDigits.forEach { char ->
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
