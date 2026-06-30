package com.msa.professionalmap.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Android broadcast adapter for provider/airplane-mode changes.
 *
 * The repository owns the policy decision, while this class owns Android receiver lifecycle and
 * API-level registration differences.
 */
internal class AndroidProviderChangeMonitor(
    context: Context,
    private val scope: CoroutineScope,
    private val onProviderChanged: suspend () -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LocationManager.PROVIDERS_CHANGED_ACTION,
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> scope.launch { onProviderChanged() }
            }
        }
    }

    fun start() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    override fun close() {
        if (!receiverRegistered) return
        runCatching { appContext.unregisterReceiver(receiver) }
        receiverRegistered = false
    }
}
