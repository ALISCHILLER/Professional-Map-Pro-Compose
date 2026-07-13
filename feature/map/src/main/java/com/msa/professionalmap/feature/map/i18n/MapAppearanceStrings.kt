package com.msa.professionalmap.feature.map.i18n

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode

internal fun MapStrings.appLanguageLabel(target: AppLanguage): String = when (target) {
    AppLanguage.English -> english
    AppLanguage.Persian -> persian
}

internal fun MapStrings.appLanguageOptionDescription(target: AppLanguage): String = when (language) {
    GuidanceLanguage.English -> "Switch application language to ${appLanguageLabel(target)}"
    GuidanceLanguage.Persian -> "تغییر زبان برنامه به ${appLanguageLabel(target)}"
}

internal fun MapStrings.themeModeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.System -> themeSystem
    AppThemeMode.Light -> themeLight
    AppThemeMode.Dark -> themeDark
}

internal fun MapStrings.themeOptionDescription(mode: AppThemeMode): String = when (language) {
    GuidanceLanguage.English -> "Use ${themeModeLabel(mode)} theme"
    GuidanceLanguage.Persian -> "استفاده از تم ${themeModeLabel(mode)}"
}
