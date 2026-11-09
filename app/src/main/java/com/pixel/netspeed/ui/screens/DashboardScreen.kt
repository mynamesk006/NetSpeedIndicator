package com.pixel.netspeed.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.data.model.NetworkStatus
import com.pixel.netspeed.data.model.NetworkType
import com.pixel.netspeed.domain.SpeedCalculator
import com.pixel.netspeed.ui.theme.DownloadGreen
import com.pixel.netspeed.ui.theme.UploadBlue
import com.pixel.netspeed.ui.viewmodel.PermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settings: AppSettings,
    speed: NetworkSpeed,
    networkStatus: NetworkStatus,
    permissionState: PermissionState,
    onMonitoringToggle: (Boolean) -> Unit,
    onOverlayToggle: (Boolean) -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pixel NetSpeed",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAppearance) {
                        Icon(Icons.Default.Palette, contentDescription = "Appearance")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Permission Warning Card (if permissions missing)
            if (!permissionState.hasOverlayPermission || !permissionState.hasNotificationPermission) {
                PermissionWarningCard(
                    permissionState = permissionState,
                    onRequestPermissions = onRequestPermissions
                )
            }

            // Live Speed Display Card
            LiveSpeedCard(speed = speed, settings = settings)

            // Main Toggles Card
            ControlTogglesCard(
                settings = settings,
                permissionState = permissionState,
                onMonitoringToggle = onMonitoringToggle,
                onOverlayToggle = onOverlayToggle,
                onRequestPermissions = onRequestPermissions
            )

            // Active Network Status Card
            NetworkStatusCard(networkStatus = networkStatus)

            // Quick Actions & QS Tile Guide
            QuickSettingsTileCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LiveSpeedCard(
    speed: NetworkSpeed,
    settings: AppSettings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REAL-TIME NETWORK TRAFFIC",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Download Metric
                SpeedMeterItem(
                    title = "Download",
                    speedText = SpeedCalculator.formatSpeed(speed.downloadBytesPerSec, settings.speedUnit),
                    icon = Icons.Default.ArrowDownward,
                    color = DownloadGreen
                )

                // Upload Metric
                SpeedMeterItem(
                    title = "Upload",
                    speedText = SpeedCalculator.formatSpeed(speed.uploadBytesPerSec, settings.speedUnit),
                    icon = Icons.Default.ArrowUpward,
                    color = UploadBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Combined Metric
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Total: ${SpeedCalculator.formatSpeed(speed.combinedBytesPerSec, settings.speedUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SpeedMeterItem(
    title: String,
    speedText: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = speedText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ControlTogglesCard(
    settings: AppSettings,
    permissionState: PermissionState,
    onMonitoringToggle: (Boolean) -> Unit,
    onOverlayToggle: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Network Monitoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Runs continuous speed monitoring in foreground service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.isMonitoringEnabled,
                    onCheckedChange = onMonitoringToggle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Floating Overlay Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!permissionState.hasOverlayPermission) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onRequestPermissions()
                            }
                        } else {
                            onOverlayToggle(!settings.isOverlayEnabled)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status Bar Floating Speed Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (permissionState.hasOverlayPermission)
                            "Displays live network speed widget right below status bar"
                        else
                            "Tap to grant 'Display over other apps' permission",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (permissionState.hasOverlayPermission)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = settings.isOverlayEnabled && permissionState.hasOverlayPermission,
                    onCheckedChange = { checked ->
                        if (!permissionState.hasOverlayPermission) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onRequestPermissions()
                            }
                        } else {
                            onOverlayToggle(checked)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NetworkStatusCard(networkStatus: NetworkStatus) {
    val (icon: ImageVector, tint: Color) = when (networkStatus.type) {
        NetworkType.WIFI -> Icons.Default.Wifi to DownloadGreen
        NetworkType.CELLULAR -> Icons.Default.NetworkCheck to UploadBlue
        NetworkType.VPN -> Icons.Default.Lock to MaterialTheme.colorScheme.primary
        NetworkType.ETHERNET -> Icons.Default.NetworkCheck to DownloadGreen
        NetworkType.DISCONNECTED -> Icons.Default.SignalCellularOff to MaterialTheme.colorScheme.error
        NetworkType.UNKNOWN -> Icons.Default.NetworkCheck to MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Active Network: ${networkStatus.networkName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (networkStatus.isConnected) "Internet Connected" else "No Connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (networkStatus.isConnected) DownloadGreen else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    permissionState: PermissionState,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Display Over Other Apps Needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (!permissionState.hasOverlayPermission)
                        "Grant overlay permission to show floating speed right below status bar."
                    else
                        "Grant notification permission for status bar indicator.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Enable")
            }
        }
    }
}

@Composable
fun QuickSettingsTileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pixel Quick Settings Tile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add 'Pixel NetSpeed' to your Pixel Quick Settings panel for one-tap monitoring toggle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
