package com.msa.professionalmap.feature.map.ui

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.android.maps.MapView

/** Compose lifecycle, saved-state and low-memory bridge for MapLibre's Android MapView. */
@Composable
fun MapLibreView(
    scene: MapScene,
    onInteraction: (MapSceneInteraction) -> Unit,
    onMapLoadFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val savedStateOwner = LocalSavedStateRegistryOwner.current
    val latestOnInteraction by rememberUpdatedState(onInteraction)
    val latestOnMapLoadFailed by rememberUpdatedState(onMapLoadFailed)
    val renderer = remember { MapSceneRenderer() }
    val interactionResolver = remember { MapInteractionResolver() }
    val restoredState = remember(savedStateOwner) {
        savedStateOwner.savedStateRegistry.consumeRestoredStateForKey(SavedStateKey)
    }
    val mapView = rememberMapView(context, lifecycle, restoredState)

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(savedStateOwner, mapView) {
        savedStateOwner.savedStateRegistry.registerSavedStateProvider(SavedStateKey) {
            Bundle().also(mapView::onSaveInstanceState)
        }
        onDispose { savedStateOwner.savedStateRegistry.unregisterSavedStateProvider(SavedStateKey) }
    }

    DisposableEffect(mapView) {
        val listener = object : MapView.OnDidFailLoadingMapListener {
            override fun onDidFailLoadingMap(errorMessage: String) {
                renderer.invalidateStyle()
                latestOnMapLoadFailed()
            }
        }
        mapView.addOnDidFailLoadingMapListener(listener)
        onDispose { mapView.removeOnDidFailLoadingMapListener(listener) }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = true
        }
    }

    DisposableEffect(mapLibreMap) {
        val map = mapLibreMap ?: return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnMapClickListener { latLng: LatLng ->
            interactionResolver.resolve(map, latLng)?.let(latestOnInteraction)
            true
        }
        map.addOnMapClickListener(listener)
        onDispose { map.removeOnMapClickListener(listener) }
    }

    val latestScene by rememberUpdatedState(scene)
    LaunchedEffect(mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        snapshotFlow { latestScene }
            .distinctUntilChanged()
            .collect { currentScene ->
                runCatching { renderer.render(map, currentScene) }
                    .onFailure {
                        renderer.invalidateStyle()
                        latestOnMapLoadFailed()
                    }
            }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}

@Composable
private fun rememberMapView(
    context: Context,
    lifecycle: Lifecycle,
    restoredState: Bundle?,
): MapView {
    val appContext = context.applicationContext
    val mapView = remember {
        MapLibre.getInstance(appContext)
        MapView(context).apply { onCreate(restoredState) }
    }
    val lifecycleDelegate = remember(mapView) { MapViewLifecycleDelegate(mapView) }

    DisposableEffect(lifecycle, lifecycleDelegate) {
        val observer = LifecycleEventObserver { _, event -> lifecycleDelegate.onEvent(event) }
        lifecycle.addObserver(observer)
        lifecycleDelegate.syncTo(lifecycle.currentState)
        onDispose {
            lifecycle.removeObserver(observer)
            lifecycleDelegate.dispose()
        }
    }

    DisposableEffect(appContext, mapView) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = mapView.onLowMemory()
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) mapView.onLowMemory()
            }
        }
        appContext.registerComponentCallbacks(callbacks)
        onDispose { appContext.unregisterComponentCallbacks(callbacks) }
    }

    return mapView
}

private class MapViewLifecycleDelegate(private val mapView: MapView) {
    private var started = false
    private var resumed = false
    private var destroyed = false

    fun onEvent(event: Lifecycle.Event) {
        if (destroyed) return
        when (event) {
            Lifecycle.Event.ON_START -> start()
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> stop()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> Unit
        }
    }

    fun syncTo(state: Lifecycle.State) {
        if (destroyed) return
        if (state.isAtLeast(Lifecycle.State.STARTED)) start()
        if (state.isAtLeast(Lifecycle.State.RESUMED)) resume()
    }

    fun dispose() {
        if (destroyed) return
        pause()
        stop()
        destroy()
    }

    private fun start() {
        if (!started) {
            mapView.onStart()
            started = true
        }
    }

    private fun resume() {
        if (!started) start()
        if (!resumed) {
            mapView.onResume()
            resumed = true
        }
    }

    private fun pause() {
        if (resumed) {
            mapView.onPause()
            resumed = false
        }
    }

    private fun stop() {
        if (started) {
            if (resumed) pause()
            mapView.onStop()
            started = false
        }
    }

    private fun destroy() {
        if (!destroyed) {
            stop()
            mapView.onDestroy()
            destroyed = true
        }
    }
}

private const val SavedStateKey = "professional_map_maplibre_state"
