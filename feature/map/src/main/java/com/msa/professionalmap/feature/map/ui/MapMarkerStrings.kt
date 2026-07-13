package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.PoiCategory
import com.msa.professionalmap.feature.map.i18n.MapStrings

/** Keeps static marker copy localized without leaking UI concerns into the domain model. */
internal fun MapPoi.localizedForMap(strings: MapStrings): MapPoi = copy(
    title = localizedTitle(strings),
    subtitle = localizedSubtitle(strings),
)

internal fun MapPoi.localizedTitle(strings: MapStrings): String = when (strings.language) {
    GuidanceLanguage.English -> title
    GuidanceLanguage.Persian -> PersianPoiCopy[id]?.first ?: title
}

internal fun MapPoi.localizedSubtitle(strings: MapStrings): String = when (strings.language) {
    GuidanceLanguage.English -> subtitle
    GuidanceLanguage.Persian -> PersianPoiCopy[id]?.second ?: subtitle
}

internal fun PoiCategory.localizedLabel(strings: MapStrings): String = when (strings.language) {
    GuidanceLanguage.English -> when (this) {
        PoiCategory.General -> "Point of interest"
        PoiCategory.Office -> "Office"
        PoiCategory.Warehouse -> "Warehouse"
        PoiCategory.Customer -> "Customer"
        PoiCategory.Alert -> "Alert"
    }

    GuidanceLanguage.Persian -> when (this) {
        PoiCategory.General -> "نقطه مهم"
        PoiCategory.Office -> "دفتر"
        PoiCategory.Warehouse -> "انبار"
        PoiCategory.Customer -> "مشتری"
        PoiCategory.Alert -> "هشدار"
    }
}

internal fun MapStrings.markerDetailsTitle(): String = when (language) {
    GuidanceLanguage.English -> "Place details"
    GuidanceLanguage.Persian -> "جزئیات مکان"
}

internal fun MapStrings.markerCoordinateLabel(): String = when (language) {
    GuidanceLanguage.English -> "Coordinates"
    GuidanceLanguage.Persian -> "مختصات"
}

private val PersianPoiCopy = mapOf(
    "hq" to ("دفتر مرکزی" to "مرکز عملیات"),
    "office-north" to ("دفتر شمال" to "عملیات منطقه‌ای"),
    "service-center" to ("مرکز خدمات" to "پشتیبانی و اعزام"),
    "warehouse-central" to ("انبار مرکزی" to "مرکز اصلی موجودی"),
    "warehouse-east" to ("انبار شرق" to "لجستیک و آماده‌سازی"),
    "distribution-hub" to ("مرکز توزیع" to "محوطه بارگیری خروجی"),
    "customer-zone" to ("محدوده مشتریان" to "منطقه اصلی خدمات"),
    "customer-north" to ("مشتری شمال" to "ویزیت برنامه‌ریزی‌شده"),
    "customer-south" to ("مشتری جنوب" to "مقصد تحویل"),
    "priority-customer" to ("مشتری ویژه" to "توقف با اولویت بالا"),
    "checkpoint-west" to ("ایست کنترل غرب" to "نقطه کنترل مسیر"),
    "checkpoint-east" to ("ایست کنترل شرق" to "نقطه کنترل مسیر"),
    "traffic-alert" to ("هشدار ترافیک" to "احتمال تراکم مسیر"),
    "restricted-zone" to ("محدوده کنترل‌شده" to "نیازمند تأیید دسترسی"),
)
