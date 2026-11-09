package com.pixel.netspeed.data.model

/**
 * Represents current device active network transport type.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    DISCONNECTED,
    UNKNOWN
}

data class NetworkStatus(
    val type: NetworkType = NetworkType.UNKNOWN,
    val isConnected: Boolean = false,
    val isVpnActive: Boolean = false,
    val networkName: String? = null
)
