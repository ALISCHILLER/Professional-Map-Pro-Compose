package com.msa.professionalmap.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.msa.professionalmap.feature.map.domain.AppThemeMode

/**
 * Small application-level preference store for visual appearance.
 *
 * Per-app locales are persisted by AppCompat. Theme mode is stored here because Compose owns the
 * final color scheme and needs the value before the first frame is rendered.
 */
class AppAppearancePreferences(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun loadThemeMode(): AppThemeMode = AppThemeMode.fromStorageValue(
        preferences.getString(KEY_THEME_MODE, null),
    )

    fun saveThemeMode(themeMode: AppThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "professional_map_appearance"
        const val KEY_THEME_MODE = "theme_mode"
    }
}

fun AppThemeMode.toAppCompatNightMode(): Int = when (this) {
    AppThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    AppThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
    AppThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
}
