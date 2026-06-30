@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiMessage

@Composable
internal fun MetricsRow(metrics: RouteMetrics?, strings: MapStrings) {
    if (metrics == null) return
    FlowRow(
        modifier = Modifier.widthIn(max = 640.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(title = strings.metricRoute, value = strings.routeMetricKm(metrics.totalDistanceKm))
        MetricTile(title = strings.metricDirect, value = strings.routeMetricKm(metrics.directDistanceKm))
        MetricTile(title = strings.metricPoints, value = strings.localizeNumberText("${metrics.simplifiedPointCount}/${metrics.originalPointCount}"))
    }
}

@Composable
private fun MetricTile(title: String, value: String, modifier: Modifier = Modifier) {
    MapKeyValueTile(
        modifier = modifier.widthIn(min = 132.dp),
        title = title,
        value = value,
        accentColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun LoadingContent(strings: MapStrings) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MapGlassPanel(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp),
            contentDescription = strings.loadingNative,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = strings.loadingNative,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.appSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ErrorContent(message: MapUiMessage, strings: MapStrings) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MapGlassPanel(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 460.dp),
            contentDescription = strings.mapFailed,
        ) {
            Text(
                text = strings.mapFailed,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = strings.messageText(message) ?: strings.mapFailed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
