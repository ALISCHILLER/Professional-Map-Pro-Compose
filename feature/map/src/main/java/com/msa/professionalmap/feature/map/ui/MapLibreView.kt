package com.msa.professionalmap.feature.map.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.msa.professionalmap.core.model.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Compose wrapper around MapLibre's classic Android MapView.
 *
 * MapLibre is still a View-based SDK on Android, so this component owns the View interop,
 * lifecycle forwarding and click bridge while the rest of the screen remains Compose-only.
 */
@Composable
fun MapLibreView(
    scene: MapScene,
    onMapClick: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestOnMapClick by rememberUpdatedState(onMapClick)
    val renderer = remember { MapSceneRenderer() }
    val mapView = rememberMapView(context, lifecycle)

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

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
            latestOnMapClick(GeoPoint(latitude = latLng.latitude, longitude = latLng.longitude))
            true
        }
        map.addOnMapClickListener(listener)
        onDispose { map.removeOnMapClickListener(listener) }
    }

    LaunchedEffect(mapLibreMap, scene) {
        mapLibreMap?.let { renderer.render(it, scene) }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
    )
}

@Composable
private fun rememberMapView(context: Context, lifecycle: Lifecycle): MapView {
    val appContext = context.applicationContext
    val mapView = remember {
        MapLibre.getInstance(appContext)
        MapView(context).apply { onCreate(null) }
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

    return mapView
}

private class MapViewLifecycleDelegate(
    private val mapView: MapView,
) {
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
