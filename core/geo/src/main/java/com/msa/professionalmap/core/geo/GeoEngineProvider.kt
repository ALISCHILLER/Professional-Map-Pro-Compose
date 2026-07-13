package com.msa.professionalmap.core.geo

data class GeoEngineResolution(
    val engine: GeoEngine,
    val implementation: String,
    val fallbackUsed: Boolean,
)

object GeoEngineProvider {
    val resolution: GeoEngineResolution by lazy {
        runCatching { NativeGeoEngine() }
            .fold(
                onSuccess = {
                    GeoEngineResolution(
                        engine = it,
                        implementation = "native",
                        fallbackUsed = false,
                    )
                },
                onFailure = {
                    GeoEngineResolution(
                        engine = KotlinGeoEngine,
                        implementation = "kotlin",
                        fallbackUsed = true,
                    )
                },
            )
    }

    val default: GeoEngine get() = resolution.engine
}
