package com.pixel.netspeed.data.model

/**
 * Persistent user settings configuration.
 */
data class AppSettings(
    val isMonitoringEnabled: Boolean = true,
    val isOverlayEnabled: Boolean = false, // Floating screen widget disabled by default
    val samplingIntervalMs: Long = 1000L,
    val speedUnit: SpeedUnit = SpeedUnit.AUTO_BYTES,
    val displayMode: DisplayMode = DisplayMode.BOTH,
    val overlayX: Int = 40,
    val overlayY: Int = 140,
    val overlayTextSizeSp: Int = 12,
    val overlayBgOpacity: Float = 0.85f,
    val overlayBgColorHex: String = "#1E1E1E",
    val overlayTextColorHex: String = "#FFFFFF",
    val isCompactOverlay: Boolean = true,
    val smoothingFactor: Float = 0.6f,
    val hideWhenZeroSpeed: Boolean = false
)
