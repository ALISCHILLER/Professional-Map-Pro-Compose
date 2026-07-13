package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-local bridge between the foreground navigation runtime and presentation layers.
 *
 * The foreground service is the owner of navigation. UI layers only observe this state and may
 * disappear without stopping the active session.
 */
internal object NavigationRuntimeRegistry {
    private val mutableState = MutableStateFlow(NavigationRuntimeState())
    val state: StateFlow<NavigationRuntimeState> = mutableState.asStateFlow()

    fun set(value: NavigationRuntimeState) {
        mutableState.value = value
    }

    fun update(transform: (NavigationRuntimeState) -> NavigationRuntimeState) {
        mutableState.update(transform)
    }

    fun reset() {
        mutableState.value = NavigationRuntimeState()
    }
}
