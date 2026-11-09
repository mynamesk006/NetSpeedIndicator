package com.pixel.netspeed.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.model.NetworkStatus
import com.pixel.netspeed.data.model.SpeedUnit
import com.pixel.netspeed.data.repository.SystemTrafficDataSource
import com.pixel.netspeed.data.repository.UserPreferencesRepository
import com.pixel.netspeed.domain.NetworkMonitor
import com.pixel.netspeed.domain.SpeedCalculator
import com.pixel.netspeed.service.NetSpeedForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PermissionState(
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = UserPreferencesRepository(context)
    private val networkMonitor = NetworkMonitor(context)
    private val trafficDataSource = SystemTrafficDataSource()
    private val speedCalculator = SpeedCalculator()

    val settingsState: StateFlow<AppSettings> = repository.appSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val networkStatusState: StateFlow<NetworkStatus> = networkMonitor.networkStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = networkMonitor.getCurrentNetworkStatus()
    )

    private val _liveSpeed = MutableStateFlow(NetworkSpeed())
    val liveSpeed: StateFlow<NetworkSpeed> = _liveSpeed.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        checkPermissions()
        startDashboardSpeedMonitoring()
    }

    fun checkPermissions() {
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _permissionState.value = PermissionState(
            hasOverlayPermission = hasOverlay,
            hasNotificationPermission = hasNotif
        )
    }

    private fun startDashboardSpeedMonitoring() {
        viewModelScope.launch {
            // Establish baseline
            speedCalculator.calculateSpeed(trafficDataSource.getSample())
            while (isActive) {
                val interval = settingsState.value.samplingIntervalMs
                delay(interval)
                val sample = trafficDataSource.getSample()
                val speed = speedCalculator.calculateSpeed(sample)
                _liveSpeed.value = speed
            }
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMonitoringEnabled(enabled)
            if (enabled) {
                NetSpeedForegroundService.startService(context)
            } else {
                NetSpeedForegroundService.stopService(context)
            }
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateOverlayEnabled(enabled)
        }
    }

    fun setSamplingInterval(intervalMs: Long) {
        viewModelScope.launch {
            repository.updateSamplingInterval(intervalMs)
        }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch {
            repository.updateSpeedUnit(unit)
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch {
            repository.updateDisplayMode(mode)
        }
    }

    fun setOverlayPosition(x: Int, y: Int) {
        viewModelScope.launch {
            repository.updateOverlayPosition(x, y)
        }
    }

    fun updateOverlayAppearance(
        textSizeSp: Int,
        bgOpacity: Float,
        bgColorHex: String,
        textColorHex: String,
        isCompact: Boolean
    ) {
        viewModelScope.launch {
            repository.updateOverlayAppearance(
                textSizeSp = textSizeSp,
                bgOpacity = bgOpacity,
                bgColorHex = bgColorHex,
                textColorHex = textColorHex,
                isCompact = isCompact
            )
        }
    }
}
