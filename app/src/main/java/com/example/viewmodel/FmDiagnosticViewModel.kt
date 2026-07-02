package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.QualcommFmEngine
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FmDiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    private val _targetInfo = MutableStateFlow(QualcommFmEngine.getTargetDeviceInfo())
    val targetInfo: StateFlow<DeviceTargetInfo> = _targetInfo.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _stepTitle = MutableStateFlow("Ready for Diagnostic Scan")
    val stepTitle: StateFlow<String> = _stepTitle.asStateFlow()

    private val _layerDiagnostics = MutableStateFlow<List<LayerDiagnostic>>(emptyList())
    val layerDiagnostics: StateFlow<List<LayerDiagnostic>> = _layerDiagnostics.asStateFlow()

    private val _jniProbes = MutableStateFlow<List<JniLibraryProbe>>(emptyList())
    val jniProbes: StateFlow<List<JniLibraryProbe>> = _jniProbes.asStateFlow()

    private val _binderProbes = MutableStateFlow<List<BinderServiceProbe>>(emptyList())
    val binderProbes: StateFlow<List<BinderServiceProbe>> = _binderProbes.asStateFlow()

    private val _halProbes = MutableStateFlow<List<HalInterfaceProbe>>(emptyList())
    val halProbes: StateFlow<List<HalInterfaceProbe>> = _halProbes.asStateFlow()

    private val _audioProbes = MutableStateFlow<List<AudioRoutingProbe>>(emptyList())
    val audioProbes: StateFlow<List<AudioRoutingProbe>> = _audioProbes.asStateFlow()

    private val _activationDecision = MutableStateFlow<ActivationDecision?>(null)
    val activationDecision: StateFlow<ActivationDecision?> = _activationDecision.asStateFlow()

    private val _engineeringReport = MutableStateFlow<CompleteEngineeringReport?>(null)
    val engineeringReport: StateFlow<CompleteEngineeringReport?> = _engineeringReport.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // FM Player UI State
    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    private val _isRadioOn = MutableStateFlow(false)
    val isRadioOn: StateFlow<Boolean> = _isRadioOn.asStateFlow()

    private val _currentFrequency = MutableStateFlow(101.5f)
    val currentFrequency: StateFlow<Float> = _currentFrequency.asStateFlow()

    private val _isStereo = MutableStateFlow(true)
    val isStereo: StateFlow<Boolean> = _isStereo.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _signalRssi = MutableStateFlow(-64)
    val signalRssi: StateFlow<Int> = _signalRssi.asStateFlow()

    private val _rdsStationName = MutableStateFlow("K-ROCK")
    val rdsStationName: StateFlow<String> = _rdsStationName.asStateFlow()

    private val _rdsProgramType = MutableStateFlow("Rock Music")
    val rdsProgramType: StateFlow<String> = _rdsProgramType.asStateFlow()

    private val _rdsRadioText = MutableStateFlow("Playing Classic Rock Hits - High Fidelity FM")
    val rdsRadioText: StateFlow<String> = _rdsRadioText.asStateFlow()

    private val _favorites = MutableStateFlow<List<FmStation>>(
        listOf(
            FmStation(88.5f, "NPR News", "Public Radio", "Morning Edition Live"),
            FmStation(95.5f, "PLUG FM", "Electronic", "Deep Synth Beats & Bass"),
            FmStation(101.5f, "K-ROCK", "Rock Music", "Playing Classic Rock Hits"),
            FmStation(107.9f, "JAZZ 108", "Smooth Jazz", "Late Night Sax & Piano")
        )
    )
    val favorites: StateFlow<List<FmStation>> = _favorites.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private var recordingJob: Job? = null

    init {
        runFullDiagnosticScan()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun runFullDiagnosticScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _currentStep.value = 1
            _stepTitle.value = "Step 1: Probing Runtime FM Hardware & Chipset Layers..."
            delay(350)
            val layers = QualcommFmEngine.performRuntimeLayerDetection(getApplication())
            _layerDiagnostics.value = layers

            _currentStep.value = 2
            _stepTitle.value = "Step 2: Testing JNI Library Invocation & Linker Isolation..."
            delay(400)
            val jnis = QualcommFmEngine.probeJniLibraries()
            _jniProbes.value = jnis

            _currentStep.value = 3
            _stepTitle.value = "Step 3: Interrogating Android Binder Services & Permissions..."
            delay(350)
            val binders = QualcommFmEngine.probeBinderServices()
            _binderProbes.value = binders

            _currentStep.value = 4
            _stepTitle.value = "Step 4: Checking BroadcastRadio & Vendor HAL Interfaces..."
            delay(300)
            val hals = QualcommFmEngine.probeHalInterfaces()
            _halProbes.value = hals

            _currentStep.value = 5
            _stepTitle.value = "Step 5: Verifying Audio Mixer Paths & Headset Dipole Antenna..."
            delay(350)
            val audio = QualcommFmEngine.probeAudioRouting(getApplication())
            _audioProbes.value = audio

            _currentStep.value = 6
            _stepTitle.value = "Step 6: Synthesizing Final Activation Decision..."
            delay(450)
            val (decision, report) = QualcommFmEngine.evaluateActivationDecision(layers, jnis, binders, hals, audio)
            _activationDecision.value = decision
            _engineeringReport.value = report

            _currentStep.value = 8
            _stepTitle.value = "Scan Complete: ${decision.title}"
            _isScanning.value = false

            // Auto-configure sandbox mode if physical hardware direct access is restricted
            if (decision != ActivationDecision.FULLY_OPERATIONAL) {
                _isSandboxMode.value = true
            }
        }
    }

    fun toggleSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
        if (!enabled && _activationDecision.value != ActivationDecision.FULLY_OPERATIONAL) {
            _isRadioOn.value = false
            stopRecording()
        }
    }

    fun togglePower() {
        _isRadioOn.value = !_isRadioOn.value
        if (_isRadioOn.value) {
            updateRdsForFrequency(_currentFrequency.value)
        } else {
            stopRecording()
        }
    }

    fun setFrequency(freq: Float) {
        val clamped = freq.coerceIn(87.5f, 108.0f)
        val rounded = (clamped * 10).toInt() / 10f
        _currentFrequency.value = rounded
        updateRdsForFrequency(rounded)
    }

    fun seekUp() {
        var next = _currentFrequency.value + 0.2f
        if (next > 108.0f) next = 87.5f
        setFrequency(next)
    }

    fun seekDown() {
        var prev = _currentFrequency.value - 0.2f
        if (prev < 87.5f) prev = 108.0f
        setFrequency(prev)
    }

    fun toggleStereo() {
        _isStereo.value = !_isStereo.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleFavorite(station: FmStation? = null) {
        val targetFreq = station?.frequency ?: _currentFrequency.value
        val currentList = _favorites.value.toMutableList()
        val index = currentList.indexOfFirst { kotlin.math.abs(it.frequency - targetFreq) < 0.05f }
        if (index >= 0) {
            currentList.removeAt(index)
        } else {
            currentList.add(
                FmStation(
                    frequency = targetFreq,
                    name = _rdsStationName.value,
                    genre = _rdsProgramType.value,
                    rdsText = _rdsRadioText.value,
                    isFavorite = true
                )
            )
        }
        _favorites.value = currentList.sortedBy { it.frequency }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _recordingSeconds.value = 0
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingSeconds.value += 1
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun updateRdsForFrequency(freq: Float) {
        val fav = _favorites.value.find { kotlin.math.abs(it.frequency - freq) < 0.05f }
        if (fav != null) {
            _rdsStationName.value = fav.name
            _rdsProgramType.value = fav.genre
            _rdsRadioText.value = fav.rdsText
            _signalRssi.value = -58
        } else {
            val roundedInt = (freq * 10).toInt()
            if (roundedInt % 5 == 0) {
                _rdsStationName.value = "FM ${String.format(java.util.Locale.US, "%.1f", freq)}"
                _rdsProgramType.value = "Top 40 & Pop"
                _rdsRadioText.value = "Live Broadcast - Snapdragon FM Tuner Reception"
                _signalRssi.value = -68
            } else {
                _rdsStationName.value = "STATIC"
                _rdsProgramType.value = "Weak RF Carrier"
                _rdsRadioText.value = "Seeking strong FM pilot tone..."
                _signalRssi.value = -95
            }
        }
    }
}
