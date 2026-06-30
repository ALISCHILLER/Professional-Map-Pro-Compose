package com.msa.professionalmap.feature.map.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.feature.map.di.rememberDefaultMapFeatureDependencies
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapLoadState
import com.msa.professionalmap.feature.map.presentation.MapViewModel
import com.msa.professionalmap.feature.map.presentation.MapViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    appMonitor: AppMonitor? = null,
) {
    val dependencies = rememberDefaultMapFeatureDependencies(appMonitor = appMonitor)
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(dependencies = dependencies)
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loadState = state.loadState

    val strings = remember(state.guidanceConfig.language) {
        MapStrings.forLanguage(state.guidanceConfig.language)
    }
    val layoutDirection = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val requestedRuntimePermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = requestedRuntimePermissions,
    )

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

    var permissionRequestLaunched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionLevel, permissionRequestLaunched) {
        when {
            permissionLevel != LocationPermissionLevel.None -> {
                viewModel.onLocationPermissionChanged(permissionLevel = permissionLevel, autoStart = true)
            }
            permissionRequestLaunched -> {
                viewModel.onLocationPermissionChanged(permissionLevel = LocationPermissionLevel.None, autoStart = false)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
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
                    )

                    MapLoadState.Ready -> ReadyMapContent(
                        state = state,
                        strings = strings,
                        permissionLevel = permissionLevel,
                        onStyleSelected = viewModel::selectStyle,
                        onMapClick = viewModel::onMapClicked,
                        onClearSelection = viewModel::clearSelectedPoint,
                        onIncreaseSimplification = viewModel::increaseSimplification,
                        onDecreaseSimplification = viewModel::decreaseSimplification,
                        onRequestLocationPermission = {
                            permissionRequestLaunched = true
                            locationPermissions.launchMultiplePermissionRequest()
                        },
                        onStartLocation = viewModel::startLocationTracking,
                        onStopLocation = viewModel::stopLocationTracking,
                        onToggleFollowUser = viewModel::toggleFollowUserLocation,
                        onCalculateRoute = { viewModel.calculateRouteToSelectedPoint(useCurrentLocation = true) },
                        onSelectRouteAlternative = viewModel::selectRouteAlternative,
                        onResetReferenceRoute = viewModel::resetReferenceRoute,
                        onStartNavigation = viewModel::startNavigation,
                        onStopNavigation = viewModel::stopNavigation,
                        onToggleGuidanceMuted = viewModel::toggleVoiceGuidanceMuted,
                        onGuidanceLanguageSelected = viewModel::setGuidanceLanguage,
                        onIncreaseGuidanceVolume = viewModel::increaseGuidanceVolume,
                        onDecreaseGuidanceVolume = viewModel::decreaseGuidanceVolume,
                        onTestGuidance = viewModel::testVoiceGuidance,
                        onRefreshOffline = viewModel::refreshOfflineRegions,
                        onDownloadOfflineRoute = viewModel::downloadCurrentRouteOffline,
                        onPauseOffline = viewModel::pauseOfflineRegion,
                        onResumeOffline = viewModel::resumeOfflineRegion,
                        onDeleteOffline = viewModel::deleteOfflineRegion,
                        onClearAmbientCache = viewModel::clearAmbientCache,
                        onPackOfflineDatabase = viewModel::packOfflineDatabase,
                    )
                }
            }
        }
    }
}
