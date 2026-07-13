package com.msa.professionalmap.core.mapdata

data class MapStyleConfig(
    val id: String,
    val title: String,
    val url: String,
    val isDark: Boolean = false,
) {
    companion object {
        val Liberty = MapStyleConfig(
            id = "liberty",
            title = "Liberty",
            url = "https://tiles.openfreemap.org/styles/liberty",
        )
        val Bright = MapStyleConfig(
            id = "bright",
            title = "Bright",
            url = "https://tiles.openfreemap.org/styles/bright",
        )
        val Positron = MapStyleConfig(
            id = "positron",
            title = "Positron",
            url = "https://tiles.openfreemap.org/styles/positron",
        )
        val Dark = MapStyleConfig(
            id = "dark",
            title = "Dark",
            url = "https://tiles.openfreemap.org/styles/dark",
            isDark = true,
        )

        val DefaultStyles = listOf(Liberty, Bright, Positron, Dark)
    }
}
