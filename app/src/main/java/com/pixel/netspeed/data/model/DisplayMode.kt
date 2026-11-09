package com.pixel.netspeed.data.model

/**
 * Enum defining which speed indicators to display in notification & overlay.
 */
enum class DisplayMode(val label: String) {
    BOTH("Download & Upload"),
    DOWNLOAD_ONLY("Download Only"),
    UPLOAD_ONLY("Upload Only"),
    COMBINED("Combined Total")
}
