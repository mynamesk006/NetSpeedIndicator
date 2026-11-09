package com.pixel.netspeed

import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.SpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default AppSettings has correct defaults`() {
        val settings = AppSettings()

        assertTrue(settings.isMonitoringEnabled)
        assertFalse(settings.isOverlayEnabled)
        assertEquals(1000L, settings.samplingIntervalMs)
        assertEquals(SpeedUnit.AUTO_BYTES, settings.speedUnit)
        assertEquals(DisplayMode.BOTH, settings.displayMode)
        assertEquals(40, settings.overlayX)
        assertEquals(140, settings.overlayY)
        assertEquals(12, settings.overlayTextSizeSp)
        assertEquals(0.85f, settings.overlayBgOpacity)
    }

    @Test
    fun `AppSettings copy retains un-modified values`() {
        val original = AppSettings()
        val modified = original.copy(samplingIntervalMs = 2000L, speedUnit = SpeedUnit.MBPS_BIT)

        assertEquals(2000L, modified.samplingIntervalMs)
        assertEquals(SpeedUnit.MBPS_BIT, modified.speedUnit)
        assertEquals(original.overlayX, modified.overlayX)
        assertEquals(original.isMonitoringEnabled, modified.isMonitoringEnabled)
    }
}
