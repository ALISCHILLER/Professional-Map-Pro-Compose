package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RoutingResult

/**
 * Converts a provider/fallback calculation outcome into an application-level route update.
 *
 * A preview-only fallback is useful when the user is planning a route, but it must not replace an
 * actively navigated provider route during reroute failure. This use case enforces that policy in
 * one place so navigation controllers cannot accidentally promote an unsafe preview into guidance.
 */
class ApplyRouteCalculationOutcomeUseCase(
    private val routePresentationBuilder: BuildRoutePresentationUseCase,
) {
    operator fun invoke(input: RouteCalculationApplicationInput): RouteCalculationApplicationResult {
        return when (val outcome = input.outcome) {
            is RouteCalculationOutcome.ProviderRoute -> providerRoute(outcome.result, input.simplificationToleranceMeters)
            is RouteCalculationOutcome.FallbackRoute -> fallbackRoute(outcome, input)
        }
    }

    private fun providerRoute(
        result: RoutingResult,
        simplificationToleranceMeters: Double,
    ): RouteCalculationApplicationResult.ProviderRouteApplied {
        val selectedRoute = result.primaryRoute
        return RouteCalculationApplicationResult.ProviderRouteApplied(
            result = result,
            selectedRoute = selectedRoute,
            alternatives = result.routes,
            presentation = routePresentationBuilder.fromAlternative(
                alternative = selectedRoute,
                simplificationToleranceMeters = simplificationToleranceMeters,
            ),
        )
    }

    private fun fallbackRoute(
        outcome: RouteCalculationOutcome.FallbackRoute,
        input: RouteCalculationApplicationInput,
    ): RouteCalculationApplicationResult {
        if (!input.allowPreviewFallback) {
            return RouteCalculationApplicationResult.ProviderFailedDuringActiveNavigation(outcome.cause)
        }
        return RouteCalculationApplicationResult.FallbackPreviewApplied(
            route = outcome.route,
            cause = outcome.cause,
            presentation = routePresentationBuilder.fromAlternative(
                alternative = outcome.route,
                simplificationToleranceMeters = input.simplificationToleranceMeters,
            ),
        )
    }
}

data class RouteCalculationApplicationInput(
    val outcome: RouteCalculationOutcome,
    val simplificationToleranceMeters: Double,
    val allowPreviewFallback: Boolean,
)

sealed interface RouteCalculationApplicationResult {
    data class ProviderRouteApplied(
        val result: RoutingResult,
        val selectedRoute: RouteAlternative,
        val alternatives: List<RouteAlternative>,
        val presentation: RoutePresentation,
    ) : RouteCalculationApplicationResult

    data class FallbackPreviewApplied(
        val route: RouteAlternative,
        val cause: Throwable,
        val presentation: RoutePresentation,
    ) : RouteCalculationApplicationResult

    data class ProviderFailedDuringActiveNavigation(
        val cause: Throwable,
    ) : RouteCalculationApplicationResult
}
