@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.appLanguageLabel
import com.msa.professionalmap.feature.map.i18n.appLanguageOptionDescription
import com.msa.professionalmap.feature.map.i18n.themeModeLabel
import com.msa.professionalmap.feature.map.i18n.themeOptionDescription

@Composable
internal fun MapAppearanceSettingsCard(
    strings: MapStrings,
    appLanguage: AppLanguage,
    themeMode: AppThemeMode,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
) {
    CommandCard(
        title = strings.sectionAppearance,
        subtitle = strings.appearanceSubtitle,
    ) {
        SettingGroupTitle(
            title = strings.appLanguage,
            subtitle = strings.appLanguageSubtitle,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    modifier = Modifier.semantics {
                        contentDescription = strings.appLanguageOptionDescription(language)
                    },
                    selected = appLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    leadingIcon = {
                        MapGlyphIcon(
                            glyph = MapGlyph.Voice,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 17.dp,
                        )
                    },
                    label = { Text(strings.appLanguageLabel(language)) },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

        SettingGroupTitle(
            title = strings.theme,
            subtitle = strings.themeSubtitle,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    modifier = Modifier.semantics {
                        contentDescription = strings.themeOptionDescription(mode)
                    },
                    selected = themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    leadingIcon = {
                        MapGlyphIcon(
                            glyph = when (mode) {
                                AppThemeMode.System -> MapGlyph.Target
                                AppThemeMode.Light -> MapGlyph.Map
                                AppThemeMode.Dark -> MapGlyph.Navigation
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            size = 17.dp,
                        )
                    },
                    label = { Text(strings.themeModeLabel(mode)) },
                )
            }
        }
    }
}

@Composable
private fun SettingGroupTitle(
    title: String,
    subtitle: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
