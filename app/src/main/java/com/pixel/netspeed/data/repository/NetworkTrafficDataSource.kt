package com.pixel.netspeed.data.repository

import android.net.TrafficStats
import android.os.SystemClock

data class TrafficSample(
    val rxBytes: Long,
    val txBytes: Long,
    val elapsedRealtimeMs: Long
)

interface NetworkTrafficDataSource {
    fun getSample(): TrafficSample
}

class SystemTrafficDataSource : NetworkTrafficDataSource {
    override fun getSample(): TrafficSample {
        return TrafficSample(
            rxBytes = TrafficStats.getTotalRxBytes(),
            txBytes = TrafficStats.getTotalTxBytes(),
            elapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
    }
}
