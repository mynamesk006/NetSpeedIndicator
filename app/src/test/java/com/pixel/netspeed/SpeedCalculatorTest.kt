package com.pixel.netspeed

import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.model.SpeedUnit
import com.pixel.netspeed.data.repository.TrafficSample
import com.pixel.netspeed.domain.SpeedCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeedCalculatorTest {

    private lateinit var calculator: SpeedCalculator

    @Before
    fun setUp() {
        calculator = SpeedCalculator(alpha = 1.0f) // Alpha 1.0 = raw speed for deterministic testing
    }

    @Test
    fun `first sample establishes baseline and returns zero speed`() {
        val sample1 = TrafficSample(rxBytes = 1000L, txBytes = 500L, elapsedRealtimeMs = 1000L)
        val speed = calculator.calculateSpeed(sample1)

        assertEquals(0L, speed.downloadBytesPerSec)
        assertEquals(0L, speed.uploadBytesPerSec)
        assertTrue(speed.isAvailable)
    }

    @Test
    fun `subsequent sample calculates correct download and upload speeds`() {
        // Baseline at t=1000ms
        calculator.calculateSpeed(TrafficSample(rxBytes = 1000L, txBytes = 500L, elapsedRealtimeMs = 1000L))

        // Sample at t=2000ms (1 second later) -> +10240 bytes RX (10 KB/s), +5120 bytes TX (5 KB/s)
        val sample2 = TrafficSample(rxBytes = 11240L, txBytes = 5620L, elapsedRealtimeMs = 2000L)
        val speed = calculator.calculateSpeed(sample2)

        assertEquals(10240L, speed.downloadBytesPerSec)
        assertEquals(5120L, speed.uploadBytesPerSec)
        assertTrue(speed.isAvailable)
    }

    @Test
    fun `counter reset or device reboot handles negative deltas gracefully`() {
        // Baseline
        calculator.calculateSpeed(TrafficSample(rxBytes = 50000L, txBytes = 20000L, elapsedRealtimeMs = 10000L))

        // Counter reset (e.g., reboot or interface toggle where RX/TX drops to 100)
        val sampleAfterReboot = TrafficSample(rxBytes = 100L, txBytes = 50L, elapsedRealtimeMs = 11000L)
        val speed = calculator.calculateSpeed(sampleAfterReboot)

        assertEquals(0L, speed.downloadBytesPerSec)
        assertEquals(0L, speed.uploadBytesPerSec)
        assertTrue(speed.isAvailable)
    }

    @Test
    fun `unsupported traffic stats returns unavailable speed`() {
        val sampleUnsupported = TrafficSample(rxBytes = -1L, txBytes = -1L, elapsedRealtimeMs = 1000L)
        val speed = calculator.calculateSpeed(sampleUnsupported)

        assertFalse(speed.isAvailable)
        assertEquals(0L, speed.downloadBytesPerSec)
        assertEquals(0L, speed.uploadBytesPerSec)
    }

    @Test
    fun `formatSpeed AUTO_BYTES formats bytes kilobytes megabytes correctly`() {
        assertEquals("0 KB/s", SpeedCalculator.formatSpeed(0L, SpeedUnit.AUTO_BYTES))
        assertEquals("500 B/s", SpeedCalculator.formatSpeed(500L, SpeedUnit.AUTO_BYTES))
        assertEquals("12.5 KB/s", SpeedCalculator.formatSpeed(12800L, SpeedUnit.AUTO_BYTES))
        assertEquals("150 KB/s", SpeedCalculator.formatSpeed(153600L, SpeedUnit.AUTO_BYTES))
        assertEquals("1.5 MB/s", SpeedCalculator.formatSpeed(1572864L, SpeedUnit.AUTO_BYTES))
    }

    @Test
    fun `formatSpeed KBPS_BIT calculates kilobits correctly`() {
        // 125,000 bytes/sec * 8 = 1,000,000 bits/sec = 1.0 Mbps
        assertEquals("1.0 Mbps", SpeedCalculator.formatSpeed(125000L, SpeedUnit.KBPS_BIT))
        // 10,000 bytes/sec * 8 = 80,000 bits/sec = 80 Kbps
        assertEquals("80 Kbps", SpeedCalculator.formatSpeed(10000L, SpeedUnit.KBPS_BIT))
    }

    @Test
    fun `formatDisplayText outputs correct layout string for display modes`() {
        val speed = NetworkSpeed(downloadBytesPerSec = 10240L, uploadBytesPerSec = 2048L)

        val bothStr = SpeedCalculator.formatDisplayText(speed, SpeedUnit.AUTO_BYTES, DisplayMode.BOTH)
        assertEquals("↓ 10.0 KB/s  ↑ 2.0 KB/s", bothStr)

        val downStr = SpeedCalculator.formatDisplayText(speed, SpeedUnit.AUTO_BYTES, DisplayMode.DOWNLOAD_ONLY)
        assertEquals("↓ 10.0 KB/s", downStr)

        val upStr = SpeedCalculator.formatDisplayText(speed, SpeedUnit.AUTO_BYTES, DisplayMode.UPLOAD_ONLY)
        assertEquals("↑ 2.0 KB/s", upStr)

        val combStr = SpeedCalculator.formatDisplayText(speed, SpeedUnit.AUTO_BYTES, DisplayMode.COMBINED)
        assertEquals("12.0 KB/s", combStr)
    }

    @Test
    fun `smoothing factor averages rapid fluctuations correctly`() {
        val smoothCalculator = SpeedCalculator(alpha = 0.5f)

        // Baseline t=0
        smoothCalculator.calculateSpeed(TrafficSample(0L, 0L, 0L))

        // t=1000ms: 100 KB/s (102,400 bytes) -> Smoothed = 0.5 * 102400 + 0.5 * 0 = 51200
        val speed1 = smoothCalculator.calculateSpeed(TrafficSample(102400L, 0L, 1000L))
        assertEquals(51200L, speed1.downloadBytesPerSec)

        // t=2000ms: 100 KB/s again -> Smoothed = 0.5 * 102400 + 0.5 * 51200 = 76800
        val speed2 = smoothCalculator.calculateSpeed(TrafficSample(204800L, 0L, 2000L))
        assertEquals(76800L, speed2.downloadBytesPerSec)
    }
}
