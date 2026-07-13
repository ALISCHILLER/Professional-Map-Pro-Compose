# JNI bridge. Keep native method owners and public names stable.
-keep class com.msa.professionalmap.core.geo.NativeGeoEngine { *; }

# MapLibre uses reflection/native bridges internally.
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Kotlin serialization DTOs used by OSRM provider.
-keepclassmembers class com.msa.professionalmap.core.routing.** { *; }

# Foreground navigation service entry points.
-keep class com.msa.professionalmap.core.service.** { *; }

# WorkManager workers are created reflectively.
-keep class com.msa.professionalmap.core.offline.data.OfflineDownloadWorker { *; }

# General native/JNI safety. R8 can rename Kotlin wrappers, but native method owner names must stay stable.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Firebase/observability wrappers.
-keep class com.msa.professionalmap.core.observability.** { *; }
-dontwarn com.google.firebase.**

# Preserve Kotlin metadata/annotations used by serialization and reflection-based SDK internals.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class **$Companion { *; }

# Remove verbose/debug logging in optimized release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
