package com.msa.professionalmap.core.offline.data

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal data class OfflineDownloadPayload(
    val request: OfflineRegionRequest,
    val languageTag: String,
)

/** Compact, deterministic payload format encrypted before it enters WorkManager's database. */
internal object OfflineDownloadPayloadCodec {
    fun encode(request: OfflineRegionRequest, languageTag: String): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeInt(Version)
            stream.writeUTF(request.clientId)
            stream.writeUTF(request.title)
            stream.writeUTF(request.styleUrl)
            stream.writeDouble(request.bounds.southWest.latitude)
            stream.writeDouble(request.bounds.southWest.longitude)
            stream.writeDouble(request.bounds.northEast.latitude)
            stream.writeDouble(request.bounds.northEast.longitude)
            stream.writeDouble(request.minZoom)
            stream.writeDouble(request.maxZoom)
            stream.writeFloat(request.pixelRatio)
            stream.writeBoolean(request.includeIdeographs)
            stream.writeUTF(languageTag.ifBlank { OfflineDownloadMessages.DefaultLanguageTag })
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): OfflineDownloadPayload = DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
        require(stream.readInt() == Version) { "Unsupported offline request version." }
        val clientId = stream.readUTF()
        val title = stream.readUTF()
        val styleUrl = stream.readUTF()
        val bounds = GeoBounds(
            southWest = GeoPoint(stream.readDouble(), stream.readDouble()),
            northEast = GeoPoint(stream.readDouble(), stream.readDouble()),
        )
        val request = OfflineRegionRequest(
            clientId = clientId,
            title = title,
            styleUrl = styleUrl,
            bounds = bounds,
            minZoom = stream.readDouble(),
            maxZoom = stream.readDouble(),
            pixelRatio = stream.readFloat(),
            includeIdeographs = stream.readBoolean(),
        )
        OfflineDownloadPayload(
            request = request,
            languageTag = stream.readUTF().ifBlank { OfflineDownloadMessages.DefaultLanguageTag },
        )
    }

    private const val Version = 1
}
