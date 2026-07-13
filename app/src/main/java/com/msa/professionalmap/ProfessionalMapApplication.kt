package com.msa.professionalmap

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.msa.professionalmap.core.observability.data.FirebaseAppMonitor
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor
import com.msa.professionalmap.core.observability.domain.MonitorEvents
import com.msa.professionalmap.ui.settings.AppAppearancePreferences
import com.msa.professionalmap.ui.settings.toAppCompatNightMode

class ProfessionalMapApplication : Application() {
    private var _appMonitor: AppMonitor = DisabledAppMonitor
    val appMonitor: AppMonitor get() = _appMonitor

    override fun onCreate() {
        super.onCreate()

        val savedThemeMode = AppAppearancePreferences(this).loadThemeMode()
        AppCompatDelegate.setDefaultNightMode(savedThemeMode.toAppCompatNightMode())

        _appMonitor = FirebaseAppMonitor.createOrDisabled(
            context = this,
            collectionEnabled = BuildConfig.FIREBASE_CONFIGURED &&
                BuildConfig.TELEMETRY_DEFAULT_ENABLED &&
                !BuildConfig.DEBUG,
        )
        appMonitor.logEvent(
            name = MonitorEvents.APP_STARTED,
            params = mapOf(
                "build_type" to if (BuildConfig.DEBUG) "debug" else "release",
                "version_name" to BuildConfig.VERSION_NAME,
                "version_code" to BuildConfig.VERSION_CODE.toString(),
            ),
        )
    }
}

fun Application.appMonitorOrDefault(): AppMonitor =
    (this as? ProfessionalMapApplication)?.appMonitor ?: DisabledAppMonitor
