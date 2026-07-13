@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.offline.domain.OfflineWorkProgress
import com.msa.professionalmap.feature.map.i18n.MapStrings

@Composable
internal fun CommandCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            content()
        }
    }
}

@Composable
internal fun CommandActions(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
internal fun PrimaryCommand(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    MapPrimaryAction(label = label, onClick = onClick, enabled = enabled)
}

@Composable
internal fun SecondaryCommand(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    MapSecondaryAction(label = label, onClick = onClick, enabled = enabled)
}

@Composable
internal fun RouteSummary(route: RouteAlternative, strings: MapStrings) {
    Text(
        text = strings.selectedRouteSummary(
            title = route.title,
            distanceKm = route.distanceKm,
            durationMinutes = route.durationMinutes,
            stepCount = route.legs.sumOf { it.steps.size },
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun OfflineJobRow(
    job: OfflineWorkProgress,
    strings: MapStrings,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = strings.offlineWorkerText(job),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryCommand(strings.pause, { onPause(job.clientId) })
            SecondaryCommand(strings.resume, { onResume(job.clientId) })
            SecondaryCommand(strings.delete, { onDelete(job.clientId) })
        }
    }
}
