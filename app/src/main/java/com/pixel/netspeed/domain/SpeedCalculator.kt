package com.pixel.netspeed.domain

import android.net.TrafficStats
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.model.SpeedUnit
import com.pixel.netspeed.data.repository.TrafficSample
import java.util.Locale
import kotlin.math.roundToLong

class SpeedCalculator(
    private var alpha: Float = 0.6f
) {
    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var lastTimestampMs: Long = -1L

    private var smoothedRxSpeed: Double = 0.0
    private var smoothedTxSpeed: Double = 0.0

    fun setSmoothingFactor(factor: Float) {
        this.alpha = factor.coerceIn(0.1f, 1.0f)
    }

    fun reset() {
        lastRxBytes = -1L
        lastTxBytes = -1L
        lastTimestampMs = -1L
        smoothedRxSpeed = 0.0
        smoothedTxSpeed = 0.0
    }

    fun calculateSpeed(sample: TrafficSample): NetworkSpeed {
        if (sample.rxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            sample.txBytes == TrafficStats.UNSUPPORTED.toLong()
        ) {
            return NetworkSpeed(downloadBytesPerSec = 0L, uploadBytesPerSec = 0L, isAvailable = false)
        }

        // First sample setup baseline
        if (lastRxBytes < 0L || lastTxBytes < 0L || lastTimestampMs < 0L) {
            lastRxBytes = sample.rxBytes
            lastTxBytes = sample.txBytes
            lastTimestampMs = sample.elapsedRealtimeMs
            return NetworkSpeed(downloadBytesPerSec = 0L, uploadBytesPerSec = 0L, isAvailable = true)
        }

        val timeDeltaMs = sample.elapsedRealtimeMs - lastTimestampMs
        if (timeDeltaMs <= 0) {
            return NetworkSpeed(
                downloadBytesPerSec = smoothedRxSpeed.roundToLong().coerceAtLeast(0L),
                uploadBytesPerSec = smoothedTxSpeed.roundToLong().coerceAtLeast(0L),
                isAvailable = true
            )
        }

        val rxDelta = sample.rxBytes - lastRxBytes
        val txDelta = sample.txBytes - lastTxBytes

        lastRxBytes = sample.rxBytes
        lastTxBytes = sample.txBytes
        lastTimestampMs = sample.elapsedRealtimeMs

        // Handle counter reset / reboot / overflow gracefully
        if (rxDelta < 0 || txDelta < 0) {
            smoothedRxSpeed = 0.0
            smoothedTxSpeed = 0.0
            return NetworkSpeed(downloadBytesPerSec = 0L, uploadBytesPerSec = 0L, isAvailable = true)
        }

        val rawRxSpeed = (rxDelta * 1000.0) / timeDeltaMs
        val rawTxSpeed = (txDelta * 1000.0) / timeDeltaMs

        // Exponential moving average filter
        smoothedRxSpeed = (alpha * rawRxSpeed) + ((1.0 - alpha) * smoothedRxSpeed)
        smoothedTxSpeed = (alpha * rawTxSpeed) + ((1.0 - alpha) * smoothedTxSpeed)

        val finalRx = if (smoothedRxSpeed < 1.0) 0L else smoothedRxSpeed.roundToLong()
        val finalTx = if (smoothedTxSpeed < 1.0) 0L else smoothedTxSpeed.roundToLong()

        return NetworkSpeed(
            downloadBytesPerSec = finalRx,
            uploadBytesPerSec = finalTx,
            isAvailable = true
        )
    }

    companion object {
        fun formatSpeed(bytesPerSec: Long, unit: SpeedUnit): String {
            if (bytesPerSec <= 0L) {
                return when (unit) {
                    SpeedUnit.KBPS_BIT, SpeedUnit.MBPS_BIT -> "0 Kbps"
                    SpeedUnit.KIB_BYTE -> "0 KiB/s"
                    SpeedUnit.MIB_BYTE -> "0 MiB/s"
                    SpeedUnit.B_BYTE -> "0 B/s"
                    SpeedUnit.AUTO_BYTES -> "0 KB/s"
                }
            }

            return when (unit) {
                SpeedUnit.AUTO_BYTES -> {
                    val bytes = bytesPerSec.toDouble()
                    when {
                        bytes < 1024 -> String.format(Locale.US, "%.0f B/s", bytes)
                        bytes < 1024 * 1024 -> {
                            val kb = bytes / 1024.0
                            if (kb < 100) String.format(Locale.US, "%.1f KB/s", kb)
                            else String.format(Locale.US, "%.0f KB/s", kb)
                        }
                        bytes < 1024 * 1024 * 1024 -> {
                            val mb = bytes / (1024.0 * 1024.0)
                            if (mb < 100) String.format(Locale.US, "%.1f MB/s", mb)
                            else String.format(Locale.US, "%.0f MB/s", mb)
                        }
                        else -> {
                            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                            String.format(Locale.US, "%.2f GB/s", gb)
                        }
                    }
                }
                SpeedUnit.KBPS_BIT -> {
                    val bitsPerSec = bytesPerSec * 8.0
                    val kbps = bitsPerSec / 1000.0
                    if (kbps >= 1000.0) {
                        String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
                    } else {
                        String.format(Locale.US, "%.0f Kbps", kbps)
                    }
                }
                SpeedUnit.MBPS_BIT -> {
                    val bitsPerSec = bytesPerSec * 8.0
                    val mbps = bitsPerSec / 1000000.0
                    String.format(Locale.US, "%.1f Mbps", mbps)
                }
                SpeedUnit.KIB_BYTE -> {
                    val kib = bytesPerSec / 1024.0
                    String.format(Locale.US, "%.1f KiB/s", kib)
                }
                SpeedUnit.MIB_BYTE -> {
                    val mib = bytesPerSec / (1024.0 * 1024.0)
                    String.format(Locale.US, "%.2f MiB/s", mib)
                }
                SpeedUnit.B_BYTE -> {
                    String.format(Locale.US, "%d B/s", bytesPerSec)
                }
            }
        }

        fun formatDisplayText(
            speed: NetworkSpeed,
            unit: SpeedUnit,
            mode: DisplayMode
        ): String {
            if (!speed.isAvailable) {
                return "Speed N/A"
            }
            val downStr = "↓ ${formatSpeed(speed.downloadBytesPerSec, unit)}"
            val upStr = "↑ ${formatSpeed(speed.uploadBytesPerSec, unit)}"
            val combStr = formatSpeed(speed.combinedBytesPerSec, unit)

            return when (mode) {
                DisplayMode.BOTH -> "$downStr  $upStr"
                DisplayMode.DOWNLOAD_ONLY -> downStr
                DisplayMode.UPLOAD_ONLY -> upStr
                DisplayMode.COMBINED -> combStr
            }
        }
    }
}
