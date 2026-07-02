package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.QualcommFmTheme
import com.example.viewmodel.FmDiagnosticViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FmDiagnosticViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QualcommFmTheme(darkTheme = true) {
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val targetInfo by viewModel.targetInfo.collectAsStateWithLifecycle()
                val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
                val stepTitle by viewModel.stepTitle.collectAsStateWithLifecycle()
                val layerDiagnostics by viewModel.layerDiagnostics.collectAsStateWithLifecycle()
                val jniProbes by viewModel.jniProbes.collectAsStateWithLifecycle()
                val binderProbes by viewModel.binderProbes.collectAsStateWithLifecycle()
                val halProbes by viewModel.halProbes.collectAsStateWithLifecycle()
                val audioProbes by viewModel.audioProbes.collectAsStateWithLifecycle()
                val activationDecision by viewModel.activationDecision.collectAsStateWithLifecycle()
                val engineeringReport by viewModel.engineeringReport.collectAsStateWithLifecycle()

                // FM Player States
                val isSandboxMode by viewModel.isSandboxMode.collectAsStateWithLifecycle()
                val isRadioOn by viewModel.isRadioOn.collectAsStateWithLifecycle()
                val currentFrequency by viewModel.currentFrequency.collectAsStateWithLifecycle()
                val isStereo by viewModel.isStereo.collectAsStateWithLifecycle()
                val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
                val signalRssi by viewModel.signalRssi.collectAsStateWithLifecycle()
                val rdsStationName by viewModel.rdsStationName.collectAsStateWithLifecycle()
                val rdsProgramType by viewModel.rdsProgramType.collectAsStateWithLifecycle()
                val rdsRadioText by viewModel.rdsRadioText.collectAsStateWithLifecycle()
                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
                val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "QUALCOMM FM ENGINE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${targetInfo.model} • Snapdragon 695",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            actions = {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.runFullDiagnosticScan() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Run Scan")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                                label = { Text("Overview") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.Default.Code, contentDescription = "JNI & Binder") },
                                label = { Text("JNI/Binder") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.Headset, contentDescription = "Audio/HAL") },
                                label = { Text("Audio/HAL") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.Radio, contentDescription = "FM Tuner") },
                                label = { Text("FM Tuner") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { viewModel.selectTab(4) },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "Report") },
                                label = { Text("Report") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Crossfade(targetState = selectedTab, label = "ScreenTransition") { tab ->
                            when (tab) {
                                0 -> DiagnosticDashboardScreen(
                                    targetInfo = targetInfo,
                                    isScanning = isScanning,
                                    currentStep = currentStep,
                                    stepTitle = stepTitle,
                                    layerDiagnostics = layerDiagnostics,
                                    activationDecision = activationDecision,
                                    onRunScan = { viewModel.runFullDiagnosticScan() },
                                    onNavigateToPlayer = { viewModel.selectTab(3) }
                                )
                                1 -> JniBinderProbeScreen(
                                    jniProbes = jniProbes,
                                    binderProbes = binderProbes
                                )
                                2 -> AudioHalScreen(
                                    halProbes = halProbes,
                                    audioProbes = audioProbes
                                )
                                3 -> FmPlayerScreen(
                                    activationDecision = activationDecision,
                                    isSandboxMode = isSandboxMode,
                                    isRadioOn = isRadioOn,
                                    currentFrequency = currentFrequency,
                                    isStereo = isStereo,
                                    isMuted = isMuted,
                                    signalRssi = signalRssi,
                                    rdsStationName = rdsStationName,
                                    rdsProgramType = rdsProgramType,
                                    rdsRadioText = rdsRadioText,
                                    favorites = favorites,
                                    isRecording = isRecording,
                                    recordingSeconds = recordingSeconds,
                                    onToggleSandbox = { viewModel.toggleSandboxMode(it) },
                                    onTogglePower = { viewModel.togglePower() },
                                    onSetFrequency = { viewModel.setFrequency(it) },
                                    onSeekUp = { viewModel.seekUp() },
                                    onSeekDown = { viewModel.seekDown() },
                                    onToggleStereo = { viewModel.toggleStereo() },
                                    onToggleMute = { viewModel.toggleMute() },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onToggleRecording = { viewModel.toggleRecording() }
                                )
                                4 -> EngineeringReportScreen(
                                    report = engineeringReport
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
