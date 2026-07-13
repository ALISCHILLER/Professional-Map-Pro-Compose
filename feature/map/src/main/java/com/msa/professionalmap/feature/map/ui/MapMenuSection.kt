package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.feature.map.i18n.MapStrings

internal enum class MapMenuSection {
    Settings,
    Map,
    Location,
    Routing,
    Navigation,
    Guidance,
    Offline,
}

internal fun MapMenuSection.label(strings: MapStrings): String = when (this) {
    MapMenuSection.Settings -> strings.sectionAppearance
    MapMenuSection.Map -> strings.mapStyle
    MapMenuSection.Location -> strings.sectionLocation
    MapMenuSection.Routing -> strings.sectionRouting
    MapMenuSection.Navigation -> strings.sectionNavigation
    MapMenuSection.Guidance -> strings.sectionGuidance
    MapMenuSection.Offline -> strings.sectionOffline
}

internal fun MapMenuSection.shortLabel(strings: MapStrings): String = when (strings.language) {
    GuidanceLanguage.English -> when (this) {
        MapMenuSection.Settings -> "Settings"
        MapMenuSection.Map -> "Map"
        MapMenuSection.Location -> "Locate"
        MapMenuSection.Routing -> "Route"
        MapMenuSection.Navigation -> "Nav"
        MapMenuSection.Guidance -> "Voice"
        MapMenuSection.Offline -> "Offline"
    }
    GuidanceLanguage.Persian -> when (this) {
        MapMenuSection.Settings -> "تنظیمات"
        MapMenuSection.Map -> "نقشه"
        MapMenuSection.Location -> "موقعیت"
        MapMenuSection.Routing -> "مسیر"
        MapMenuSection.Navigation -> "ناوبری"
        MapMenuSection.Guidance -> "صدا"
        MapMenuSection.Offline -> "آفلاین"
    }
}

internal val MapMenuSection.glyph: MapGlyph
    get() = when (this) {
        MapMenuSection.Settings -> MapGlyph.Menu
        MapMenuSection.Map -> MapGlyph.Map
        MapMenuSection.Location -> MapGlyph.Location
        MapMenuSection.Routing -> MapGlyph.Route
        MapMenuSection.Navigation -> MapGlyph.Navigation
        MapMenuSection.Guidance -> MapGlyph.Voice
        MapMenuSection.Offline -> MapGlyph.Offline
    }
