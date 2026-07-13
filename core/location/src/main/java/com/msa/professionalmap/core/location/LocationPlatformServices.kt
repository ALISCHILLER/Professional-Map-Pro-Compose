package com.msa.professionalmap.core.location

/**
 * Reads runtime permission state from the Android edge.
 *
 * Keeping this behind an interface lets the repository enforce tracking policy without directly
 * knowing how Android permission APIs are queried. The concrete implementation remains in the
 * Android adapter and tests can inject deterministic readers through the internal constructor.
 */
internal fun interface LocationPermissionReader {
    fun detectPermissionLevel(): LocationPermissionLevel
}

/**
 * Reads device provider state from the Android edge.
 */
internal fun interface LocationProviderStateReader {
    fun readProviderState(): LocationProvidersState
}
