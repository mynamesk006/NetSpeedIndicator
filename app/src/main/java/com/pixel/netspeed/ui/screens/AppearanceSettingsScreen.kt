package com.pixel.netspeed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.SpeedUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onAppearanceUpdate: (textSizeSp: Int, bgOpacity: Float, bgColorHex: String, textColorHex: String, isCompact: Boolean) -> Unit,
    onResetPosition: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            // Widget Preview Card
            WidgetPreviewCard(settings = settings)

            // Speed Unit Selector
            SpeedUnitCard(currentUnit = settings.speedUnit, onSelectUnit = onSpeedUnitChange)

            // Display Mode Selector
            DisplayModeCard(currentMode = settings.displayMode, onSelectMode = onDisplayModeChange)

            // Size & Opacity Card
            SizeAndOpacityCard(
                settings = settings,
                onUpdate = onAppearanceUpdate
            )

            // Color Presets Card
            ColorPresetsCard(
                settings = settings,
                onUpdate = onAppearanceUpdate
            )

            // Reset Position Action
            OutlinedButton(
                onClick = onResetPosition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Floating Position (Default Top-Left)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WidgetPreviewCard(settings: AppSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FLOATING WIDGET PREVIEW",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Render Preview Box according to user settings
            val parsedBgColor = parseColorWithAlpha(settings.overlayBgColorHex, settings.overlayBgOpacity)
            val parsedTextColor = parseColorHex(settings.overlayTextColorHex)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(parsedBgColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                val downStr = "↓ 14.2 MB/s"
                val upStr = "↑ 2.8 MB/s"
                val combStr = "⇅ 17.0 MB/s"

                if (settings.isCompactOverlay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (settings.displayMode) {
                            DisplayMode.BOTH -> {
                                Text(downStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(upStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.DOWNLOAD_ONLY -> {
                                Text(downStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.UPLOAD_ONLY -> {
                                Text(upStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.COMBINED -> {
                                Text(combStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        when (settings.displayMode) {
                            DisplayMode.BOTH -> {
                                Text(downStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(upStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.DOWNLOAD_ONLY -> {
                                Text(downStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.UPLOAD_ONLY -> {
                                Text(upStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                            DisplayMode.COMBINED -> {
                                Text(combStr, color = parsedTextColor, fontSize = settings.overlayTextSizeSp.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedUnitCard(
    currentUnit: SpeedUnit,
    onSelectUnit: (SpeedUnit) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Speed Unit Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SpeedUnit.entries.forEach { unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectUnit(unit) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (unit == currentUnit),
                        onClick = { onSelectUnit(unit) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = unit.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun DisplayModeCard(
    currentMode: DisplayMode,
    onSelectMode: (DisplayMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Display Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            DisplayMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (mode == currentMode),
                        onClick = { onSelectMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = mode.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SizeAndOpacityCard(
    settings: AppSettings,
    onUpdate: (textSizeSp: Int, bgOpacity: Float, bgColorHex: String, textColorHex: String, isCompact: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Size & Transparency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text Size Slider
            Text(
                text = "Text Size: ${settings.overlayTextSizeSp} sp",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.overlayTextSizeSp.toFloat(),
                onValueChange = { size ->
                    onUpdate(size.toInt(), settings.overlayBgOpacity, settings.overlayBgColorHex, settings.overlayTextColorHex, settings.isCompactOverlay)
                },
                valueRange = 8f..22f,
                steps = 14
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Opacity Slider
            Text(
                text = "Background Opacity: ${(settings.overlayBgOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.overlayBgOpacity,
                onValueChange = { opacity ->
                    onUpdate(settings.overlayTextSizeSp, opacity, settings.overlayBgColorHex, settings.overlayTextColorHex, settings.isCompactOverlay)
                },
                valueRange = 0.1f..1.0f
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Layout Format Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compact Horizontal Layout",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = settings.isCompactOverlay,
                    onCheckedChange = { compact ->
                        onUpdate(settings.overlayTextSizeSp, settings.overlayBgOpacity, settings.overlayBgColorHex, settings.overlayTextColorHex, compact)
                    }
                )
            }
        }
    }
}

@Composable
fun ColorPresetsCard(
    settings: AppSettings,
    onUpdate: (textSizeSp: Int, bgOpacity: Float, bgColorHex: String, textColorHex: String, isCompact: Boolean) -> Unit
) {
    val bgColors = listOf("#1E1E1E", "#000000", "#121212", "#0D47A1", "#1B5E20", "#4A148C")
    val textColors = listOf("#FFFFFF", "#00E676", "#00B0FF", "#FFD600", "#FF3D00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Color Customization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Background Color", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bgColors.forEach { colorHex ->
                    val isSelected = settings.overlayBgColorHex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseColorHex(colorHex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                onUpdate(
                                    settings.overlayTextSizeSp,
                                    settings.overlayBgOpacity,
                                    colorHex,
                                    settings.overlayTextColorHex,
                                    settings.isCompactOverlay
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Text Color", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                textColors.forEach { colorHex ->
                    val isSelected = settings.overlayTextColorHex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseColorHex(colorHex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                onUpdate(
                                    settings.overlayTextSizeSp,
                                    settings.overlayBgOpacity,
                                    settings.overlayBgColorHex,
                                    colorHex,
                                    settings.isCompactOverlay
                                )
                            }
                    )
                }
            }
        }
    }
}

fun parseColorHex(colorHex: String): Color {
    return try {
        val parsedInt = android.graphics.Color.parseColor(colorHex)
        Color(parsedInt)
    } catch (e: Exception) {
        Color.White
    }
}

fun parseColorWithAlpha(colorHex: String, opacity: Float): Color {
    val base = parseColorHex(colorHex)
    return base.copy(alpha = opacity.coerceIn(0.1f, 1.0f))
}
