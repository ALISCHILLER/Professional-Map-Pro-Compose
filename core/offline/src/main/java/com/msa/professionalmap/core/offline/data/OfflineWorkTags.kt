package com.msa.professionalmap.core.offline.data

internal object OfflineWorkTags {
    const val AllDownloadsTag = "offline-map-download"
    private const val ClientTagPrefix = "offline-map-client:"

    fun clientTag(clientId: String): String = ClientTagPrefix + clientId

    fun clientIdFrom(tag: String): String? = tag
        .takeIf { it.startsWith(ClientTagPrefix) }
        ?.removePrefix(ClientTagPrefix)
        ?.takeIf { it.isNotBlank() }
}
