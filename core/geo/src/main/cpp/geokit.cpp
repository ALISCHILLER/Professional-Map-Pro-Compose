#include <jni.h>
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <vector>

namespace {
constexpr double kEarthRadiusMeters = 6371008.8;
constexpr double kPi = 3.141592653589793238462643383279502884;

double to_radians(double degrees) { return degrees * kPi / 180.0; }
double to_degrees(double radians) { return radians * 180.0 / kPi; }
double square(double value) { return value * value; }

double normalize_longitude(double longitude) {
    double result = std::fmod(longitude + 540.0, 360.0);
    if (result < 0.0) result += 360.0;
    return result - 180.0;
}

struct Point {
    double lat;
    double lon;
};

void validate_lat_lon(double lat, double lon) {
    if (!std::isfinite(lat) || !std::isfinite(lon) || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
        throw std::invalid_argument("Invalid coordinate.");
    }
}

double distance_meters(Point from, Point to) {
    validate_lat_lon(from.lat, from.lon);
    validate_lat_lon(to.lat, to.lon);
    const double lat1 = to_radians(from.lat);
    const double lat2 = to_radians(to.lat);
    const double dlat = to_radians(to.lat - from.lat);
    const double dlon = to_radians(to.lon - from.lon);
    const double raw_a = square(std::sin(dlat / 2.0)) +
        std::cos(lat1) * std::cos(lat2) * square(std::sin(dlon / 2.0));
    const double a = std::min(1.0, std::max(0.0, raw_a));
    const double c = 2.0 * std::atan2(std::sqrt(a), std::sqrt(1.0 - a));
    return kEarthRadiusMeters * c;
}

double initial_bearing_degrees(Point from, Point to) {
    validate_lat_lon(from.lat, from.lon);
    validate_lat_lon(to.lat, to.lon);
    const double lat1 = to_radians(from.lat);
    const double lat2 = to_radians(to.lat);
    const double dlon = to_radians(to.lon - from.lon);
    const double y = std::sin(dlon) * std::cos(lat2);
    const double x = std::cos(lat1) * std::sin(lat2) -
        std::sin(lat1) * std::cos(lat2) * std::cos(dlon);
    double bearing = std::fmod(to_degrees(std::atan2(y, x)) + 360.0, 360.0);
    if (bearing < 0.0) bearing += 360.0;
    return bearing;
}

Point destination_point(Point from, double bearing_degrees, double distance) {
    validate_lat_lon(from.lat, from.lon);
    if (!std::isfinite(bearing_degrees) || !std::isfinite(distance)) {
        throw std::invalid_argument("Invalid bearing or distance.");
    }
    const double angular_distance = distance / kEarthRadiusMeters;
    const double bearing = to_radians(bearing_degrees);
    const double lat1 = to_radians(from.lat);
    const double lon1 = to_radians(from.lon);
    const double lat2 = std::asin(std::sin(lat1) * std::cos(angular_distance) +
        std::cos(lat1) * std::sin(angular_distance) * std::cos(bearing));
    const double lon2 = lon1 + std::atan2(
        std::sin(bearing) * std::sin(angular_distance) * std::cos(lat1),
        std::cos(angular_distance) - std::sin(lat1) * std::sin(lat2)
    );
    return Point{to_degrees(lat2), normalize_longitude(to_degrees(lon2))};
}

std::vector<Point> from_flat(JNIEnv* env, jdoubleArray array) {
    const jsize size = env->GetArrayLength(array);
    if (size % 2 != 0) {
        throw std::invalid_argument("Flat coordinate array must contain lat/lon pairs.");
    }
    std::vector<jdouble> flat(static_cast<size_t>(size));
    env->GetDoubleArrayRegion(array, 0, size, flat.data());
    std::vector<Point> points;
    points.reserve(static_cast<size_t>(size / 2));
    for (jsize i = 0; i < size; i += 2) {
        const Point point{flat[static_cast<size_t>(i)], flat[static_cast<size_t>(i + 1)]};
        validate_lat_lon(point.lat, point.lon);
        points.push_back(point);
    }
    return points;
}

jdoubleArray to_flat(JNIEnv* env, const std::vector<Point>& points) {
    std::vector<jdouble> flat;
    flat.reserve(points.size() * 2);
    for (const Point& point : points) {
        flat.push_back(point.lat);
        flat.push_back(point.lon);
    }
    jdoubleArray result = env->NewDoubleArray(static_cast<jsize>(flat.size()));
    env->SetDoubleArrayRegion(result, 0, static_cast<jsize>(flat.size()), flat.data());
    return result;
}

double route_length_meters(const std::vector<Point>& points) {
    if (points.size() < 2) return 0.0;
    double sum = 0.0;
    for (size_t i = 1; i < points.size(); ++i) {
        sum += distance_meters(points[i - 1], points[i]);
    }
    return sum;
}

double cross_track_distance_meters(Point point, Point start, Point end) {
    const double d13 = distance_meters(start, point) / kEarthRadiusMeters;
    const double theta13 = to_radians(initial_bearing_degrees(start, point));
    const double theta12 = to_radians(initial_bearing_degrees(start, end));
    return std::abs(std::asin(std::sin(d13) * std::sin(theta13 - theta12)) * kEarthRadiusMeters);
}

void simplify_recursive(
    const std::vector<Point>& points,
    size_t start,
    size_t end,
    double tolerance,
    std::vector<bool>& keep
) {
    if (end <= start + 1) return;
    double max_distance = -1.0;
    size_t max_index = start;
    for (size_t i = start + 1; i < end; ++i) {
        const double distance = cross_track_distance_meters(points[i], points[start], points[end]);
        if (distance > max_distance) {
            max_distance = distance;
            max_index = i;
        }
    }
    if (max_distance > tolerance) {
        keep[max_index] = true;
        simplify_recursive(points, start, max_index, tolerance, keep);
        simplify_recursive(points, max_index, end, tolerance, keep);
    }
}

std::vector<Point> simplify_route(const std::vector<Point>& points, double tolerance) {
    if (points.size() <= 2 || tolerance <= 0.0) return points;
    std::vector<bool> keep(points.size(), false);
    keep.front() = true;
    keep.back() = true;
    simplify_recursive(points, 0, points.size() - 1, tolerance, keep);
    std::vector<Point> result;
    result.reserve(points.size());
    for (size_t i = 0; i < points.size(); ++i) {
        if (keep[i]) result.push_back(points[i]);
    }
    return result;
}

void throw_java(JNIEnv* env, const char* class_name, const char* message) {
    jclass clazz = env->FindClass(class_name);
    if (clazz != nullptr) env->ThrowNew(clazz, message);
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeSelfTest(JNIEnv*, jobject) {
    const double tehran_to_karaj = distance_meters(Point{35.6892, 51.3890}, Point{35.8327, 50.9915});
    return tehran_to_karaj > 30000.0 && tehran_to_karaj < 50000.0;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeDistanceMeters(
    JNIEnv* env,
    jobject,
    jdouble from_lat,
    jdouble from_lon,
    jdouble to_lat,
    jdouble to_lon
) {
    try {
        return distance_meters(Point{from_lat, from_lon}, Point{to_lat, to_lon});
    } catch (const std::exception& ex) {
        throw_java(env, "java/lang/IllegalArgumentException", ex.what());
        return 0.0;
    }
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeInitialBearingDegrees(
    JNIEnv* env,
    jobject,
    jdouble from_lat,
    jdouble from_lon,
    jdouble to_lat,
    jdouble to_lon
) {
    try {
        return initial_bearing_degrees(Point{from_lat, from_lon}, Point{to_lat, to_lon});
    } catch (const std::exception& ex) {
        throw_java(env, "java/lang/IllegalArgumentException", ex.what());
        return 0.0;
    }
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeDestinationPoint(
    JNIEnv* env,
    jobject,
    jdouble from_lat,
    jdouble from_lon,
    jdouble bearing_degrees,
    jdouble distance_meters_value
) {
    try {
        const Point point = destination_point(Point{from_lat, from_lon}, bearing_degrees, distance_meters_value);
        const jdouble values[2] = {point.lat, point.lon};
        jdoubleArray result = env->NewDoubleArray(2);
        env->SetDoubleArrayRegion(result, 0, 2, values);
        return result;
    } catch (const std::exception& ex) {
        throw_java(env, "java/lang/IllegalArgumentException", ex.what());
        return env->NewDoubleArray(0);
    }
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeRouteLengthMeters(
    JNIEnv* env,
    jobject,
    jdoubleArray flat_lat_lon
) {
    try {
        return route_length_meters(from_flat(env, flat_lat_lon));
    } catch (const std::exception& ex) {
        throw_java(env, "java/lang/IllegalArgumentException", ex.what());
        return 0.0;
    }
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_msa_professionalmap_core_geo_NativeGeoEngine_nativeSimplifyRoute(
    JNIEnv* env,
    jobject,
    jdoubleArray flat_lat_lon,
    jdouble tolerance_meters
) {
    try {
        if (!std::isfinite(tolerance_meters) || tolerance_meters < 0.0) {
            throw std::invalid_argument("Tolerance must be non-negative.");
        }
        return to_flat(env, simplify_route(from_flat(env, flat_lat_lon), tolerance_meters));
    } catch (const std::exception& ex) {
        throw_java(env, "java/lang/IllegalArgumentException", ex.what());
        return env->NewDoubleArray(0);
    }
}
