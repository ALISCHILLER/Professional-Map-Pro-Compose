@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.offline.domain.OfflineDownloadProgress
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineWorkManagerState
import com.msa.professionalmap.feature.map.i18n.MapStrings

@Composable
internal fun OfflinePanel(
    strings: MapStrings,
    offlineState: OfflineManagerState,
    offlineWorkState: OfflineWorkManagerState,
    onRefreshOffline: () -> Unit,
    onDownloadOfflineRoute: () -> Unit,
    onPauseOffline: (String) -> Unit,
    onResumeOffline: (String) -> Unit,
    onDeleteOffline: (String) -> Unit,
    onClearAmbientCache: () -> Unit,
    onPackOfflineDatabase: () -> Unit,
) {
    MapSectionCard(
        title = strings.sectionOffline,
        subtitle = strings.offlineStatusText(offlineState),
        accentColor = MaterialTheme.colorScheme.secondary,
        trailing = {
            if (offlineState.isBusy) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp), strokeWidth = 2.dp)
            } else {
                StatusPill(text = strings.refresh)
            }
        },
    ) {
        if (offlineWorkState.activeJobs.isNotEmpty()) {
            offlineWorkState.activeJobs.forEach { job ->
                Text(
                    text = strings.offlineWorkerText(job),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (offlineState.activeDownloads.isNotEmpty()) {
            offlineState.activeDownloads.forEach { progress ->
                OfflineDownloadRow(
                    progress = progress,
                    strings = strings,
                    onPauseOffline = onPauseOffline,
                    onResumeOffline = onResumeOffline,
                    onDeleteOffline = onDeleteOffline,
                )
            }
        }

        if (offlineState.regions.isNotEmpty()) {
            StatusPill(text = "${strings.savedRegions}: ${strings.localizeNumberText(offlineState.regions.size.toString())}")
            offlineState.regions.take(3).forEach { region ->
                Text(
                    text = "• ${region.title} · ${strings.percentText(region.progressFraction)} · ${strings.localizeNumberText(region.completedTileCount.toString())} ${strings.tiles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        MapQuickActionBar {
            MapPrimaryAction(label = strings.downloadRoute, onClick = onDownloadOfflineRoute)
            MapSecondaryAction(label = strings.list, onClick = onRefreshOffline)
            MapSecondaryAction(label = strings.clearCache, onClick = onClearAmbientCache)
            MapSecondaryAction(label = strings.packDb, onClick = onPackOfflineDatabase)
        }
    }
}

@Composable
private fun OfflineDownloadRow(
    progress: OfflineDownloadProgress,
    strings: MapStrings,
    onPauseOffline: (String) -> Unit,
    onResumeOffline: (String) -> Unit,
    onDeleteOffline: (String) -> Unit,
) {
    MapSectionCard(
        title = progress.title,
        subtitle = progress.errorMessage
            ?: "${strings.percentText(progress.progressPercent.toDouble() / 100.0)} · ${strings.localizeNumberText(progress.completedTileCount.toString())} ${strings.tiles} · ${strings.bytesText(progress.completedBytes)}",
        accentColor = if (progress.errorMessage == null) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        trailing = {
            StatusPill(text = strings.percentText(progress.progressFraction))
        },
    ) {
        LinearProgressIndicator(
            progress = { progress.progressFraction.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        ) {
            MapPrimaryAction(
                label = if (progress.isDownloading) strings.pause else strings.resume,
                onClick = {
                    if (progress.isDownloading) onPauseOffline(progress.clientId) else onResumeOffline(progress.clientId)
                },
            )
            MapSecondaryAction(
                label = strings.delete,
                onClick = { onDeleteOffline(progress.clientId) },
            )
        }
    }
}
