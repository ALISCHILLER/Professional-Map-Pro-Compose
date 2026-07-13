package com.msa.professionalmap.feature.map.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.feature.map.di.rememberDefaultMapFeatureDependencies
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapLoadState
import com.msa.professionalmap.feature.map.presentation.MapViewModel
import com.msa.professionalmap.feature.map.presentation.MapViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    themeMode: AppThemeMode,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    appMonitor: AppMonitor? = null,
) {
    val dependencies = rememberDefaultMapFeatureDependencies(appMonitor = appMonitor)
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel(factory = MapViewModelFactory(dependencies))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loadState = state.loadState

    val configuration = LocalConfiguration.current
    val appLanguage = remember(configuration) {
        AppLanguage.fromLanguageTag(configuration.locales[0].toLanguageTag())
    }
    val uiLanguage = appLanguage.toGuidanceLanguage()
    val strings = remember(uiLanguage) { MapStrings.forLanguage(uiLanguage) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val fineGranted = locationPermissions.permissions.any {
        it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted
    }
    val coarseGranted = locationPermissions.permissions.any {
        it.permission == Manifest.permission.ACCESS_COARSE_LOCATION && it.status.isGranted
    }
    val permissionLevel = when {
        fineGranted -> LocationPermissionLevel.Precise
        coarseGranted -> LocationPermissionLevel.Approximate
        else -> LocationPermissionLevel.None
    }

    var locationPermissionAttempted by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionAttempted by rememberSaveable { mutableStateOf(false) }
    var lastObservedPermissionLevel by remember { mutableStateOf<LocationPermissionLevel?>(null) }

    LaunchedEffect(permissionLevel, locationPermissionAttempted) {
        val previousPermissionLevel = lastObservedPermissionLevel
        lastObservedPermissionLevel = permissionLevel

        when {
            permissionLevel != LocationPermissionLevel.None -> {
                viewModel.onLocationPermissionChanged(permissionLevel, autoStart = true)
            }
            locationPermissionAttempted || (
                previousPermissionLevel != null &&
                    previousPermissionLevel != LocationPermissionLevel.None
            ) -> {
                viewModel.onLocationPermissionChanged(LocationPermissionLevel.None, autoStart = false)
            }
        }
    }

    LaunchedEffect(state.navigationActive, notificationPermission?.status?.isGranted) {
        if (
            state.navigationActive &&
            notificationPermission != null &&
            !notificationPermission.status.isGranted &&
            !notificationPermissionAttempted
        ) {
            notificationPermissionAttempted = true
            notificationPermission.launchPermissionRequest()
        }
    }

    MapTextDirection(strings) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (loadState != MapLoadState.Ready) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (loadState) {
                    MapLoadState.Idle,
                    MapLoadState.Loading -> LoadingContent(strings)

                    is MapLoadState.Error -> ErrorContent(
                        message = loadState.message,
                        strings = strings,
                        onRetry = viewModel::retryLoad,
                    )

                    MapLoadState.Ready -> {
                        val requestLocationPermission = remember(locationPermissions) {
                            {
                                if (locationPermissionAttempted && !locationPermissions.shouldShowRationale) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null),
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                } else {
                                    locationPermissionAttempted = true
                                    locationPermissions.launchMultiplePermissionRequest()
                                }
                            }
                        }
                        ReadyMapContent(
                            state = state,
                            strings = strings,
                            appLanguage = appLanguage,
                            themeMode = themeMode,
                            permissionLevel = permissionLevel,
                            actions = rememberMapActionHandlers(
                                viewModel = viewModel,
                                onRequestLocationPermission = requestLocationPermission,
                                onAppLanguageSelected = onAppLanguageSelected,
                                onThemeModeSelected = onThemeModeSelected,
                            ),
                        )
                    }
                }
            }
        }
    }
}
