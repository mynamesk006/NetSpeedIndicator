package com.pixel.netspeed.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.model.SpeedUnit
import java.util.Locale

object NotificationIconGenerator {

    fun generateSpeedIcon(
        context: Context,
        speed: NetworkSpeed,
        unit: SpeedUnit,
        mode: DisplayMode
    ): IconCompat {
        val bytesPerSec = when (mode) {
            DisplayMode.DOWNLOAD_ONLY -> speed.downloadBytesPerSec
            DisplayMode.UPLOAD_ONLY -> speed.uploadBytesPerSec
            DisplayMode.BOTH, DisplayMode.COMBINED -> speed.combinedBytesPerSec
        }

        val (valueText, unitText) = formatShortForIcon(bytesPerSec, unit)

        val density = context.resources.displayMetrics.density
        // Render HD icon canvas 96x96 px
        val size = (36 * density).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.52f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
        }

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.36f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
        }

        val centerX = size / 2f
        val valueY = size * 0.48f
        val unitY = size * 0.90f

        canvas.drawText(valueText, centerX, valueY, valuePaint)
        canvas.drawText(unitText, centerX, unitY, unitPaint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun formatShortForIcon(bytesPerSec: Long, unit: SpeedUnit): Pair<String, String> {
        if (bytesPerSec <= 0L) {
            return when (unit) {
                SpeedUnit.KBPS_BIT, SpeedUnit.MBPS_BIT -> Pair("0", "KB")
                SpeedUnit.KIB_BYTE -> Pair("0", "KiB")
                SpeedUnit.MIB_BYTE -> Pair("0", "MiB")
                SpeedUnit.B_BYTE -> Pair("0", "B")
                SpeedUnit.AUTO_BYTES -> Pair("0", "KB")
            }
        }

        val bytes = bytesPerSec.toDouble()
        return when (unit) {
            SpeedUnit.KBPS_BIT, SpeedUnit.MBPS_BIT -> {
                val bits = bytes * 8.0
                val kbps = bits / 1000.0
                if (kbps < 1000.0) {
                    Pair(String.format(Locale.US, "%.0f", kbps), "KB")
                } else {
                    val mbps = kbps / 1000.0
                    if (mbps < 10) Pair(String.format(Locale.US, "%.1f", mbps), "Mb")
                    else Pair(String.format(Locale.US, "%.0f", mbps), "MB")
                }
            }
            SpeedUnit.KIB_BYTE -> {
                val kib = bytes / 1024.0
                if (kib < 10) Pair(String.format(Locale.US, "%.1f", kib), "KiB")
                else Pair(String.format(Locale.US, "%.0f", kib), "KiB")
            }
            SpeedUnit.MIB_BYTE -> {
                val mib = bytes / (1024.0 * 1024.0)
                Pair(String.format(Locale.US, "%.1f", mib), "MiB")
            }
            SpeedUnit.B_BYTE -> {
                Pair(bytesPerSec.toString(), "B")
            }
            SpeedUnit.AUTO_BYTES -> {
                when {
                    bytes < 1024 -> Pair(bytesPerSec.toString(), "B")
                    bytes < 1024 * 1024 -> {
                        val kb = bytes / 1024.0
                        if (kb < 10) String.format(Locale.US, "%.1f", kb) to "KB"
                        else String.format(Locale.US, "%.0f", kb) to "KB"
                    }
                    bytes < 1024 * 1024 * 1024 -> {
                        val mb = bytes / (1024.0 * 1024.0)
                        if (mb < 10) String.format(Locale.US, "%.1f", mb) to "MB"
                        else String.format(Locale.US, "%.0f", mb) to "MB"
                    }
                    else -> {
                        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                        String.format(Locale.US, "%.1f", gb) to "GB"
                    }
                }
            }
        }
    }
}
