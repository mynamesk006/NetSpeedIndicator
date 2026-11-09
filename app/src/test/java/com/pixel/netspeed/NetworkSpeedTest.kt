package com.pixel.netspeed

import com.pixel.netspeed.data.model.NetworkSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkSpeedTest {

    @Test
    fun `combinedBytesPerSec calculates sum of download and upload`() {
        val speed = NetworkSpeed(downloadBytesPerSec = 5000L, uploadBytesPerSec = 1200L)
        assertEquals(6200L, speed.combinedBytesPerSec)
    }

    @Test
    fun `zero network speed returns zero combined`() {
        val speed = NetworkSpeed(downloadBytesPerSec = 0L, uploadBytesPerSec = 0L)
        assertEquals(0L, speed.combinedBytesPerSec)
    }
}
