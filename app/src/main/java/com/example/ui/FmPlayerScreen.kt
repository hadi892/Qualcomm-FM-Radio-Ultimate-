package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun FmPlayerScreen(
    activationDecision: ActivationDecision?,
    isSandboxMode: Boolean,
    isRadioOn: Boolean,
    currentFrequency: Float,
    isStereo: Boolean,
    isMuted: Boolean,
    signalRssi: Int,
    rdsStationName: String,
    rdsProgramType: String,
    rdsRadioText: String,
    favorites: List<FmStation>,
    isRecording: Boolean,
    recordingSeconds: Int,
    onToggleSandbox: (Boolean) -> Unit,
    onTogglePower: () -> Unit,
    onSetFrequency: (Float) -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit,
    onToggleStereo: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: (FmStation?) -> Unit,
    onToggleRecording: () -> Unit
) {
    val canAccessHardwareDirectly = activationDecision == ActivationDecision.FULLY_OPERATIONAL
    val isPlayerEnabled = canAccessHardwareDirectly || isSandboxMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode switch / lockdown card
        if (!canAccessHardwareDirectly) {
            EngineeringLockdownCard(
                decision = activationDecision,
                isSandboxMode = isSandboxMode,
                onToggleSandbox = onToggleSandbox
            )
        }

        // FM Tuner Dashboard
        FmTunerDisplayCard(
            isPlayerEnabled = isPlayerEnabled,
            isRadioOn = isRadioOn,
            frequency = currentFrequency,
            isStereo = isStereo,
            isMuted = isMuted,
            rssi = signalRssi,
            rdsName = rdsStationName,
            rdsType = rdsProgramType,
            rdsText = rdsRadioText,
            isFavorite = favorites.any { kotlin.math.abs(it.frequency - currentFrequency) < 0.05f },
            isRecording = isRecording,
            recordingSeconds = recordingSeconds
        )

        // Player Controls
        FmControlsCard(
            isPlayerEnabled = isPlayerEnabled,
            isRadioOn = isRadioOn,
            frequency = currentFrequency,
            isStereo = isStereo,
            isMuted = isMuted,
            isFavorite = favorites.any { kotlin.math.abs(it.frequency - currentFrequency) < 0.05f },
            isRecording = isRecording,
            onTogglePower = onTogglePower,
            onSetFrequency = onSetFrequency,
            onSeekUp = onSeekUp,
            onSeekDown = onSeekDown,
            onToggleStereo = onToggleStereo,
            onToggleMute = onToggleMute,
            onToggleFavorite = { onToggleFavorite(null) },
            onToggleRecording = onToggleRecording
        )

        // Favorites Shelf
        AnimatedVisibility(visible = isPlayerEnabled && isRadioOn) {
            FavoritesShelf(
                favorites = favorites,
                currentFrequency = currentFrequency,
                onSelectStation = { onSetFrequency(it.frequency) }
            )
        }
    }
}

@Composable
fun EngineeringLockdownCard(
    decision: ActivationDecision?,
    isSandboxMode: Boolean,
    onToggleSandbox: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSandboxMode) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isSandboxMode) Icons.Default.Science else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isSandboxMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isSandboxMode) "ENGINEERING UI SANDBOX ACTIVE" else "HARDWARE TUNER RESTRICTED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSandboxMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sandbox Mode",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Switch(
                        checked = isSandboxMode,
                        onCheckedChange = onToggleSandbox
                    )
                }
            }

            Text(
                text = if (isSandboxMode)
                    "You are evaluating the complete Material 3 Qualcomm FM Player interface, frequency synthesizer dial, and RDS packet stream inside the Engineering UI Sandbox."
                else
                    "Exact Engineering Reason: ${decision?.title ?: "Blocked"}. Direct access to /dev/radio0 and vendor HAL daemon is restricted by Android 16 SELinux policy. Player disabled automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FmTunerDisplayCard(
    isPlayerEnabled: Boolean,
    isRadioOn: Boolean,
    frequency: Float,
    isStereo: Boolean,
    isMuted: Boolean,
    rssi: Int,
    rdsName: String,
    rdsType: String,
    rdsText: String,
    isFavorite: Boolean,
    isRecording: Boolean,
    recordingSeconds: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Dark slate LCD screen
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, if (isRadioOn && isPlayerEnabled) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top status bar inside LCD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRadioOn && isPlayerEnabled) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (!isPlayerEnabled) "DISABLED" else if (isRadioOn) "QUALCOMM TUNER ON" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isRecording) {
                            Surface(
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "REC ${recordingSeconds / 60}:${String.format(java.util.Locale.US, "%02d", recordingSeconds % 60)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isRadioOn && isPlayerEnabled) {
                            Surface(
                                color = if (isStereo) Color(0xFF0284C7) else Color(0xFF475569),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isStereo) "STEREO" else "MONO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "$rssi dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Main Frequency Readout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRadioOn && isPlayerEnabled) String.format(java.util.Locale.US, "%.1f", frequency) else "---.-",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRadioOn && isPlayerEnabled) Color(0xFF38BDF8) else Color(0xFF475569)
                    )
                    Text(
                        text = "MHz FM BROADCAST BAND",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B),
                        letterSpacing = 2.sp
                    )
                }

                // RDS Ticker Display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (isRadioOn && isPlayerEnabled) "RDS: $rdsName" else "RDS DECODER INACTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = if (isRadioOn && isPlayerEnabled) rdsType else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Text(
                            text = if (isRadioOn && isPlayerEnabled) rdsText else "Power on receiver to stream Radio Data System (RDS) subcarrier packets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FmControlsCard(
    isPlayerEnabled: Boolean,
    isRadioOn: Boolean,
    frequency: Float,
    isStereo: Boolean,
    isMuted: Boolean,
    isFavorite: Boolean,
    isRecording: Boolean,
    onTogglePower: () -> Unit,
    onSetFrequency: (Float) -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit,
    onToggleStereo: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Slider Tuning
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "87.5 MHz", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text(text = "TUNING DIAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(text = "108.0 MHz", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }

                Slider(
                    value = frequency,
                    onValueChange = { onSetFrequency(it) },
                    valueRange = 87.5f..108.0f,
                    enabled = isPlayerEnabled && isRadioOn
                )
            }

            // Primary control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Power
                FilledIconButton(
                    onClick = onTogglePower,
                    enabled = isPlayerEnabled,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRadioOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(28.dp))
                }

                // Seek Down
                IconButton(
                    onClick = onSeekDown,
                    enabled = isPlayerEnabled && isRadioOn,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Seek Down", modifier = Modifier.size(28.dp))
                }

                // Step Down (-0.1)
                OutlinedIconButton(
                    onClick = { onSetFrequency(frequency - 0.1f) },
                    enabled = isPlayerEnabled && isRadioOn,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("-0.1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                // Step Up (+0.1)
                OutlinedIconButton(
                    onClick = { onSetFrequency(frequency + 0.1f) },
                    enabled = isPlayerEnabled && isRadioOn,
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("+0.1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                // Seek Up
                IconButton(
                    onClick = onSeekUp,
                    enabled = isPlayerEnabled && isRadioOn,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Seek Up", modifier = Modifier.size(28.dp))
                }
            }

            // Secondary controls shelf
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isFavorite,
                    onClick = onToggleFavorite,
                    enabled = isPlayerEnabled && isRadioOn,
                    label = { Text(if (isFavorite) "Favorited" else "Favorite") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                FilterChip(
                    selected = isStereo,
                    onClick = onToggleStereo,
                    enabled = isPlayerEnabled && isRadioOn,
                    label = { Text(if (isStereo) "Stereo" else "Mono") },
                    leadingIcon = {
                        Icon(Icons.Default.SurroundSound, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = isRecording,
                    onClick = onToggleRecording,
                    enabled = isPlayerEnabled && isRadioOn,
                    label = { Text(if (isRecording) "Recording" else "Record") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isRecording) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                IconButton(
                    onClick = onToggleMute,
                    enabled = isPlayerEnabled && isRadioOn
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute"
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesShelf(
    favorites: List<FmStation>,
    currentFrequency: Float,
    onSelectStation: (FmStation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PRESET STATIONS (${favorites.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(favorites) { station ->
                val isSelected = kotlin.math.abs(station.frequency - currentFrequency) < 0.05f
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onSelectStation(station) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${station.frequency} MHz",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = station.genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
