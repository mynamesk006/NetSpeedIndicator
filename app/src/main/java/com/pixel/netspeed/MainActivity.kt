package com.pixel.netspeed

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixel.netspeed.service.NetSpeedForegroundService
import com.pixel.netspeed.ui.screens.AppearanceSettingsScreen
import com.pixel.netspeed.ui.screens.DashboardScreen
import com.pixel.netspeed.ui.screens.GeneralSettingsScreen
import com.pixel.netspeed.ui.screens.PermissionGuideScreen
import com.pixel.netspeed.ui.theme.PixelNetSpeedTheme
import com.pixel.netspeed.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelNetSpeedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PixelNetSpeedApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun PixelNetSpeedApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val settings by viewModel.settingsState.collectAsState()
    val liveSpeed by viewModel.liveSpeed.collectAsState()
    val networkStatus by viewModel.networkStatusState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkPermissions()
    }

    // Auto-start foreground service if monitoring is enabled in settings
    LaunchedEffect(settings.isMonitoringEnabled) {
        if (settings.isMonitoringEnabled) {
            val context = viewModel.getApplication<android.app.Application>()
            NetSpeedForegroundService.startService(context)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                settings = settings,
                speed = liveSpeed,
                networkStatus = networkStatus,
                permissionState = permissionState,
                onMonitoringToggle = { enabled ->
                    viewModel.setMonitoringEnabled(enabled)
                },
                onOverlayToggle = { enabled ->
                    viewModel.setOverlayEnabled(enabled)
                },
                onNavigateToAppearance = {
                    navController.navigate("appearance")
                },
                onNavigateToSettings = {
                    navController.navigate("general_settings")
                },
                onRequestPermissions = {
                    navController.navigate("permissions")
                }
            )
        }

        composable("appearance") {
            AppearanceSettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onSpeedUnitChange = { unit ->
                    viewModel.setSpeedUnit(unit)
                },
                onDisplayModeChange = { mode ->
                    viewModel.setDisplayMode(mode)
                },
                onAppearanceUpdate = { textSizeSp, bgOpacity, bgColorHex, textColorHex, isCompact ->
                    viewModel.updateOverlayAppearance(
                        textSizeSp = textSizeSp,
                        bgOpacity = bgOpacity,
                        bgColorHex = bgColorHex,
                        textColorHex = textColorHex,
                        isCompact = isCompact
                    )
                },
                onResetPosition = {
                    viewModel.setOverlayPosition(40, 80)
                }
            )
        }

        composable("general_settings") {
            GeneralSettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onSamplingIntervalChange = { intervalMs ->
                    viewModel.setSamplingInterval(intervalMs)
                }
            )
        }

        composable("permissions") {
            PermissionGuideScreen(
                permissionState = permissionState,
                onBack = { navController.popBackStack() },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRefreshPermissions = {
                    viewModel.checkPermissions()
                }
            )
        }
    }
}
