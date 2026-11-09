package com.pixel.netspeed.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pixel.netspeed.MainActivity
import com.pixel.netspeed.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NetSpeedTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val canDrawOverlays = Settings.canDrawOverlays(this)
        if (!canDrawOverlays) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        serviceScope.launch {
            val currentSettings = preferencesRepository.appSettingsFlow.first()
            val newState = !currentSettings.isMonitoringEnabled
            preferencesRepository.updateMonitoringEnabled(newState)

            if (newState) {
                NetSpeedForegroundService.startService(this@NetSpeedTileService)
            } else {
                NetSpeedForegroundService.stopService(this@NetSpeedTileService)
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            val currentSettings = preferencesRepository.appSettingsFlow.first()
            val tile = qsTile ?: return@launch

            if (currentSettings.isMonitoringEnabled) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Pixel NetSpeed"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Monitoring Active"
                }
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Pixel NetSpeed"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Monitoring Off"
                }
            }
            tile.updateTile()
        }
    }
}
