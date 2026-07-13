package com.msa.professionalmap.core.offline.data

import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionDefinition
import org.maplibre.android.offline.OfflineRegionStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun OfflineManager.createOfflineRegionAwait(
    definition: OfflineRegionDefinition,
    metadata: ByteArray,
): OfflineRegion = suspendCancellableCoroutine { continuation ->
    createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
        override fun onCreate(offlineRegion: OfflineRegion) {
            if (continuation.isActive) continuation.resume(offlineRegion)
        }

        override fun onError(error: String) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error))
        }
    })
}

internal suspend fun OfflineManager.listOfflineRegionsAwait(): Array<OfflineRegion> = suspendCancellableCoroutine { continuation ->
    listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
        override fun onList(offlineRegions: Array<OfflineRegion>?) {
            if (continuation.isActive) continuation.resume(offlineRegions ?: emptyArray())
        }

        override fun onError(error: String) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error))
        }
    })
}

internal suspend fun OfflineRegion.getStatusAwait(): OfflineRegionStatus = suspendCancellableCoroutine { continuation ->
    getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
        override fun onStatus(status: OfflineRegionStatus?) {
            if (status == null) {
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(UnknownOfflineStatusError))
            } else if (continuation.isActive) {
                continuation.resume(status)
            }
        }

        override fun onError(error: String?) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error ?: UnknownOfflineError))
        }
    })
}

internal suspend fun OfflineRegion.deleteAwait(): Unit = suspendCancellableCoroutine { continuation ->
    delete(object : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun onError(error: String) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error))
        }
    })
}

internal suspend fun OfflineManager.setMaximumAmbientCacheSizeAwait(bytes: Long): Unit = suspendFileSourceCallback {
    setMaximumAmbientCacheSize(bytes, it)
}

internal suspend fun OfflineManager.clearAmbientCacheAwait(): Unit = suspendFileSourceCallback {
    clearAmbientCache(it)
}

internal suspend fun OfflineManager.invalidateAmbientCacheAwait(): Unit = suspendFileSourceCallback {
    invalidateAmbientCache(it)
}

internal suspend fun OfflineManager.packDatabaseAwait(): Unit = suspendFileSourceCallback {
    packDatabase(it)
}

private suspend fun suspendFileSourceCallback(
    block: (OfflineManager.FileSourceCallback) -> Unit,
): Unit = suspendCancellableCoroutine { continuation ->
    block(object : OfflineManager.FileSourceCallback {
        override fun onSuccess() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun onError(message: String) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
        }
    })
}


private const val UnknownOfflineError = "Unknown MapLibre offline error."
private const val UnknownOfflineStatusError = "MapLibre returned no offline region status."
