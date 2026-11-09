package com.pixel.netspeed.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pixel.netspeed.R
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.repository.SystemTrafficDataSource
import com.pixel.netspeed.data.repository.UserPreferencesRepository
import com.pixel.netspeed.domain.SpeedCalculator
import com.pixel.netspeed.ui.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetSpeedForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var trafficDataSource: SystemTrafficDataSource
    private lateinit var speedCalculator: SpeedCalculator
    private lateinit var notificationManager: NotificationManager
    private var overlayManager: OverlayManager? = null

    private var currentSettings: AppSettings = AppSettings()
    private var lastCalculatedSpeed: NetworkSpeed = NetworkSpeed()
    private var samplingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(this)
        trafficDataSource = SystemTrafficDataSource()
        speedCalculator = SpeedCalculator()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()

        overlayManager = OverlayManager(this) { x, y ->
            serviceScope.launch {
                preferencesRepository.updateOverlayPosition(x, y)
            }
        }

        // Observe settings changes continuously
        serviceScope.launch {
            preferencesRepository.appSettingsFlow.collectLatest { settings ->
                val oldSettings = currentSettings
                currentSettings = settings

                speedCalculator.setSmoothingFactor(settings.smoothingFactor)

                if (settings.isOverlayEnabled && settings.isMonitoringEnabled) {
                    overlayManager?.showOverlay(settings)
                } else {
                    overlayManager?.hideOverlay()
                }

                if (!settings.isMonitoringEnabled) {
                    stopSelf()
                    return@collectLatest
                }

                // If sampling interval changed, restart sampling loop
                if (oldSettings.samplingIntervalMs != settings.samplingIntervalMs || samplingJob?.isActive != true) {
                    restartSamplingLoop()
                }

                updateNotification(lastCalculatedSpeed)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        startForegroundInternal()
        restartSamplingLoop()

        return START_STICKY
    }

    private fun startForegroundInternal() {
        val notification = buildNotification(lastCalculatedSpeed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun restartSamplingLoop() {
        samplingJob?.cancel()
        samplingJob = serviceScope.launch {
            // Baseline sample
            speedCalculator.calculateSpeed(trafficDataSource.getSample())

            while (isActive) {
                delay(currentSettings.samplingIntervalMs)

                val sample = trafficDataSource.getSample()
                val speed = speedCalculator.calculateSpeed(sample)
                lastCalculatedSpeed = speed

                // Update Overlay UI (thread-safe inside OverlayManager)
                if (currentSettings.isOverlayEnabled) {
                    overlayManager?.updateSpeed(speed)
                }

                // Update Notification
                updateNotification(speed)
            }
        }
    }

    private fun updateNotification(speed: NetworkSpeed) {
        val notification = buildNotification(speed)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun buildNotification(speed: NetworkSpeed): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NetSpeedForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = SpeedCalculator.formatDisplayText(
            speed,
            currentSettings.speedUnit,
            currentSettings.displayMode
        )

        val dynamicSpeedIcon = NotificationIconGenerator.generateSpeedIcon(
            this,
            speed,
            currentSettings.speedUnit,
            currentSettings.displayMode
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(dynamicSpeedIcon)
            .setContentTitle("Pixel NetSpeed")
            .setContentText(speedText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_netspeed_notification,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Re-create notification channel with IMPORTANCE_DEFAULT so OS displays icon in Status Bar
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopMonitoring() {
        serviceScope.launch {
            preferencesRepository.updateMonitoringEnabled(false)
            overlayManager?.hideOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        samplingJob?.cancel()
        overlayManager?.hideOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "pixel_netspeed_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.pixel.netspeed.action.START"
        const val ACTION_STOP_SERVICE = "com.pixel.netspeed.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, NetSpeedForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetSpeedForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
