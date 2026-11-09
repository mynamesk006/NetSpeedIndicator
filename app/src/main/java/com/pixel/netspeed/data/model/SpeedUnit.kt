package com.pixel.netspeed.data.model

/**
 * Enum defining the supported unit formatting modes.
 */
enum class SpeedUnit(val label: String) {
    AUTO_BYTES("Auto (B/s, KB/s, MB/s)"),
    KBPS_BIT("Kbps / Mbps (bits)"),
    MBPS_BIT("Mbps (bits)"),
    KIB_BYTE("KiB/s (binary)"),
    MIB_BYTE("MiB/s (binary)"),
    B_BYTE("Bytes/s (raw)")
}
