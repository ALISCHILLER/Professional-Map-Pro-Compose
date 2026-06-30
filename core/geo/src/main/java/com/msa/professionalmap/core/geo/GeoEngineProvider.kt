package com.msa.professionalmap.core.geo

object GeoEngineProvider {
    val default: GeoEngine by lazy {
        runCatching { NativeGeoEngine() }.getOrElse { KotlinGeoEngine }
    }
}
