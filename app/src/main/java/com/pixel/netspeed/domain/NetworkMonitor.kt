package com.pixel.netspeed.domain

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.pixel.netspeed.data.model.NetworkStatus
import com.pixel.netspeed.data.model.NetworkType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatusFlow: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkStatus())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentNetworkStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(NetworkStatus(type = NetworkType.UNKNOWN, isConnected = false, networkName = "Error"))
        }

        // Initial status
        trySend(getCurrentNetworkStatus())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    @SuppressLint("MissingPermission")
    fun getCurrentNetworkStatus(): NetworkStatus {
        val activeNetwork = try {
            connectivityManager.activeNetwork
        } catch (e: Exception) {
            null
        } ?: return NetworkStatus(
            type = NetworkType.DISCONNECTED,
            isConnected = false,
            isVpnActive = false,
            networkName = "Disconnected"
        )

        val caps = try {
            connectivityManager.getNetworkCapabilities(activeNetwork)
        } catch (e: Exception) {
            null
        } ?: return NetworkStatus(
            type = NetworkType.DISCONNECTED,
            isConnected = false,
            isVpnActive = false,
            networkName = "No Capabilities"
        )

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            isVpn -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }

        val name = when (type) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.CELLULAR -> "Mobile Data"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.VPN -> "VPN Connection"
            NetworkType.UNKNOWN -> "Connected"
            else -> "Connected"
        }

        return NetworkStatus(
            type = type,
            isConnected = hasInternet,
            isVpnActive = isVpn,
            networkName = name
        )
    }
}
