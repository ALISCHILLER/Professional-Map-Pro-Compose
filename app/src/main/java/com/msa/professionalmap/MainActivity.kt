package com.msa.professionalmap

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.ui.MapScreen
import com.msa.professionalmap.ui.settings.AppAppearancePreferences
import com.msa.professionalmap.ui.settings.toAppCompatNightMode
import com.msa.professionalmap.ui.theme.ProfessionalMapTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appearancePreferences = remember { AppAppearancePreferences(applicationContext) }
            var themeMode by rememberSaveable {
                mutableStateOf(appearancePreferences.loadThemeMode())
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            ProfessionalMapTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MapScreen(
                        appMonitor = application.appMonitorOrDefault(),
                        themeMode = themeMode,
                        onAppLanguageSelected = ::setApplicationLanguage,
                        onThemeModeSelected = { selectedMode ->
                            if (selectedMode != themeMode) {
                                appearancePreferences.saveThemeMode(selectedMode)
                                themeMode = selectedMode
                                AppCompatDelegate.setDefaultNightMode(
                                    selectedMode.toAppCompatNightMode(),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun setApplicationLanguage(language: AppLanguage) {
        val selectedLocales = LocaleListCompat.forLanguageTags(language.languageTag)
        if (AppCompatDelegate.getApplicationLocales() != selectedLocales) {
            AppCompatDelegate.setApplicationLocales(selectedLocales)
        }
    }
}
