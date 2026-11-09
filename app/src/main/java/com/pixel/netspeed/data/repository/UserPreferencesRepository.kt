package com.pixel.netspeed.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.SpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixel_netspeed_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val SAMPLING_INTERVAL_MS = longPreferencesKey("sampling_interval_ms")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
        val OVERLAY_TEXT_SIZE_SP = intPreferencesKey("overlay_text_size_sp")
        val OVERLAY_BG_OPACITY = floatPreferencesKey("overlay_bg_opacity")
        val OVERLAY_BG_COLOR_HEX = stringPreferencesKey("overlay_bg_color_hex")
        val OVERLAY_TEXT_COLOR_HEX = stringPreferencesKey("overlay_text_color_hex")
        val OVERLAY_COMPACT = booleanPreferencesKey("overlay_compact")
        val SMOOTHING_FACTOR = floatPreferencesKey("smoothing_factor")
        val HIDE_WHEN_ZERO = booleanPreferencesKey("hide_when_zero")
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isMonitoringEnabled = prefs[Keys.MONITORING_ENABLED] ?: true,
            isOverlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false, // Default to false (only status bar icon active)
            samplingIntervalMs = prefs[Keys.SAMPLING_INTERVAL_MS] ?: 1000L,
            speedUnit = try {
                SpeedUnit.valueOf(prefs[Keys.SPEED_UNIT] ?: SpeedUnit.AUTO_BYTES.name)
            } catch (_: Exception) {
                SpeedUnit.AUTO_BYTES
            },
            displayMode = try {
                DisplayMode.valueOf(prefs[Keys.DISPLAY_MODE] ?: DisplayMode.BOTH.name)
            } catch (_: Exception) {
                DisplayMode.BOTH
            },
            overlayX = prefs[Keys.OVERLAY_X] ?: 40,
            overlayY = prefs[Keys.OVERLAY_Y] ?: 140,
            overlayTextSizeSp = prefs[Keys.OVERLAY_TEXT_SIZE_SP] ?: 12,
            overlayBgOpacity = prefs[Keys.OVERLAY_BG_OPACITY] ?: 0.85f,
            overlayBgColorHex = prefs[Keys.OVERLAY_BG_COLOR_HEX] ?: "#1E1E1E",
            overlayTextColorHex = prefs[Keys.OVERLAY_TEXT_COLOR_HEX] ?: "#FFFFFF",
            isCompactOverlay = prefs[Keys.OVERLAY_COMPACT] ?: true,
            smoothingFactor = prefs[Keys.SMOOTHING_FACTOR] ?: 0.6f,
            hideWhenZeroSpeed = prefs[Keys.HIDE_WHEN_ZERO] ?: false
        )
    }

    suspend fun updateMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun updateOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun updateSamplingInterval(intervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAMPLING_INTERVAL_MS] = intervalMs.coerceIn(500L, 3000L)
        }
    }

    suspend fun updateSpeedUnit(unit: SpeedUnit) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SPEED_UNIT] = unit.name
        }
    }

    suspend fun updateDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_MODE] = mode.name
        }
    }

    suspend fun updateOverlayPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_X] = x
            prefs[Keys.OVERLAY_Y] = y
        }
    }

    suspend fun updateOverlayAppearance(
        textSizeSp: Int = 12,
        bgOpacity: Float = 0.85f,
        bgColorHex: String = "#1E1E1E",
        textColorHex: String = "#FFFFFF",
        isCompact: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_TEXT_SIZE_SP] = textSizeSp.coerceIn(8, 24)
            prefs[Keys.OVERLAY_BG_OPACITY] = bgOpacity.coerceIn(0.1f, 1.0f)
            prefs[Keys.OVERLAY_BG_COLOR_HEX] = bgColorHex
            prefs[Keys.OVERLAY_TEXT_COLOR_HEX] = textColorHex
            prefs[Keys.OVERLAY_COMPACT] = isCompact
        }
    }
}
