package com.pixel.netspeed.data.model

/**
 * Value object representing current calculated network download and upload speeds in Bytes per second.
 */
data class NetworkSpeed(
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
) {
    val combinedBytesPerSec: Long
        get() = downloadBytesPerSec + uploadBytesPerSec
}
