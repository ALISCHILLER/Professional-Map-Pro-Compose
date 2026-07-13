package com.msa.professionalmap.feature.map.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.geo.GeoEngineProvider
import com.msa.professionalmap.core.guidance.data.DefaultVoiceGuidanceRepository
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import com.msa.professionalmap.core.guidance.domain.VoiceGuidanceRepository
import com.msa.professionalmap.core.location.AndroidFusedLocationRepository
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.core.mapdata.MapCatalogRepository
import com.msa.professionalmap.core.mapdata.StaticMapRepository
import com.msa.professionalmap.core.offline.data.AndroidMapLibreOfflineMapRepository
import com.msa.professionalmap.core.offline.data.AndroidWorkManagerOfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineMapRepository
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository
import com.msa.professionalmap.core.routing.CachedRoutingRepository
import com.msa.professionalmap.core.routing.OsrmRoutingConfig
import com.msa.professionalmap.core.routing.OsrmRoutingRepository
import com.msa.professionalmap.core.routing.RoutingRepository
import com.msa.professionalmap.core.service.data.AndroidNavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationRoutingConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.feature.map.BuildConfig
import com.msa.professionalmap.feature.map.data.SharedPreferencesLanguagePreferenceStore
import com.msa.professionalmap.feature.map.domain.LanguagePreferenceStore
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import com.msa.professionalmap.feature.map.guidance.AndroidTtsEngine
import io.ktor.client.engine.android.Android

/**
 * Runtime dependency boundary for the map feature.
 *
 * Compose code receives this explicit object instead of building dependencies in
 * presentation classes. That keeps UI orchestration deterministic, testable and
 * replaceable by Koin/Hilt or a hand-written app container later.
 */
data class MapFeatureDependencies(
    val repository: MapCatalogRepository,
    val geoEngine: GeoEngine,
    val locationRepository: LocationRepository,
    val routingRepository: RoutingRepository,
    val offlineRepository: OfflineMapRepository,
    val offlineDownloadManager: OfflineDownloadManager,
    val progressRepository: RouteProgressRepository,
    val guidanceRepository: VoiceGuidanceRepository,
    val ttsEngine: TtsEngine,
    val navigationServiceController: NavigationServiceController,
    val navigationRoutingConfig: NavigationRoutingConfig,
    val appMonitor: AppMonitor,
    val languagePreferenceStore: LanguagePreferenceStore,
    val config: MapFeatureConfig = MapFeatureConfig(),
)

@Composable
fun rememberDefaultMapFeatureDependencies(
    appMonitor: AppMonitor? = null,
    config: MapFeatureConfig = MapFeatureConfig(),
): MapFeatureDependencies {
    val context = LocalContext.current.applicationContext
    val safeMonitor = remember(appMonitor) { appMonitor ?: DisabledAppMonitor }
    return remember(context, safeMonitor, config) {
        createDefaultMapFeatureDependencies(
            context = context,
            appMonitor = safeMonitor,
            config = config,
        )
    }
}

fun createDefaultMapFeatureDependencies(
    context: Context,
    appMonitor: AppMonitor = DisabledAppMonitor,
    config: MapFeatureConfig = MapFeatureConfig(),
): MapFeatureDependencies {
    val geoResolution = GeoEngineProvider.resolution
    appMonitor.setCustomKey("geo_engine", geoResolution.implementation)
    appMonitor.logEvent(
        name = com.msa.professionalmap.core.observability.domain.MonitorEvents.GEO_ENGINE_SELECTED,
        params = mapOf(
            "implementation" to geoResolution.implementation,
            "fallback_used" to geoResolution.fallbackUsed.toString(),
        ),
    )
    return MapFeatureDependencies(
    repository = StaticMapRepository(),
    geoEngine = geoResolution.engine,
    locationRepository = AndroidFusedLocationRepository(context),
    routingRepository = CachedRoutingRepository(
        OsrmRoutingConfig(
            baseUrl = BuildConfig.OSRM_BASE_URL,
            userAgent = BuildConfig.ROUTING_USER_AGENT,
            allowCleartextTraffic = BuildConfig.ALLOW_CLEARTEXT_ROUTING,
        ).let { routingConfig ->
            OsrmRoutingRepository(
                config = routingConfig,
                client = OsrmRoutingRepository.createClient(routingConfig, Android),
            )
        }
    ),
    offlineRepository = AndroidMapLibreOfflineMapRepository(context),
    offlineDownloadManager = AndroidWorkManagerOfflineDownloadManager(context),
    progressRepository = DefaultRouteProgressRepository(),
    guidanceRepository = DefaultVoiceGuidanceRepository(),
    ttsEngine = AndroidTtsEngine(context),
    navigationServiceController = AndroidNavigationServiceController(context),
    navigationRoutingConfig = NavigationRoutingConfig(
        baseUrl = BuildConfig.OSRM_BASE_URL,
        userAgent = BuildConfig.ROUTING_USER_AGENT,
        allowCleartextTraffic = BuildConfig.ALLOW_CLEARTEXT_ROUTING,
    ),
    appMonitor = appMonitor,
    languagePreferenceStore = SharedPreferencesLanguagePreferenceStore(context),
    config = config,
    )
}
