package com.msa.professionalmap.core.geo

import com.msa.professionalmap.core.model.GeoPoint

class NativeGeoEngine : GeoEngine {

    init {
        System.loadLibrary(LIB_NAME)
        check(nativeSelfTest()) { "Native geokit self-test failed." }
    }

    override fun distanceMeters(from: GeoPoint, to: GeoPoint): Double = nativeDistanceMeters(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
    )

    override fun initialBearingDegrees(from: GeoPoint, to: GeoPoint): Double = nativeInitialBearingDegrees(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
    )

    override fun destinationPoint(
        from: GeoPoint,
        bearingDegrees: Double,
        distanceMeters: Double,
    ): GeoPoint {
        require(bearingDegrees.isFinite()) { "Bearing must be finite." }
        require(distanceMeters.isFinite() && distanceMeters >= 0.0) { "Distance must be finite and non-negative." }
        val result = nativeDestinationPoint(
            from.latitude,
            from.longitude,
            bearingDegrees,
            distanceMeters,
        )
        require(result.size == 2) { "Native destination result must contain [lat, lon]." }
        return GeoPoint(latitude = result[0], longitude = result[1])
    }

    override fun routeLengthMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        return nativeRouteLengthMeters(points.toNativeArray())
    }

    override fun simplifyRoute(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
        if (points.size <= 2) return points
        require(toleranceMeters.isFinite() && toleranceMeters >= 0.0) { "Tolerance must be finite and non-negative." }
        val result = nativeSimplifyRoute(points.toNativeArray(), toleranceMeters)
        return result.toGeoPoints()
    }

    private external fun nativeSelfTest(): Boolean
    private external fun nativeDistanceMeters(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
    ): Double

    private external fun nativeInitialBearingDegrees(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
    ): Double

    private external fun nativeDestinationPoint(
        fromLat: Double,
        fromLon: Double,
        bearingDegrees: Double,
        distanceMeters: Double,
    ): DoubleArray

    private external fun nativeRouteLengthMeters(flatLatLon: DoubleArray): Double
    private external fun nativeSimplifyRoute(flatLatLon: DoubleArray, toleranceMeters: Double): DoubleArray

    private fun List<GeoPoint>.toNativeArray(): DoubleArray {
        val result = DoubleArray(size * 2)
        forEachIndexed { index, point ->
            result[index * 2] = point.latitude
            result[index * 2 + 1] = point.longitude
        }
        return result
    }

    private fun DoubleArray.toGeoPoints(): List<GeoPoint> {
        require(size % 2 == 0) { "Flat coordinate array must contain pairs." }
        return List(size / 2) { index ->
            GeoPoint(latitude = this[index * 2], longitude = this[index * 2 + 1])
        }
    }

    companion object {
        private const val LIB_NAME = "geokit"
    }
}
