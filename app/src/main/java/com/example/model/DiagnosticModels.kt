package com.example.model

enum class ActivationDecision(
    val title: String,
    val description: String,
    val severity: DecisionSeverity
) {
    FULLY_OPERATIONAL(
        "FM fully operational",
        "All Qualcomm FM Radio layers (Kernel driver, HAL, Binder, JNI, and Audio routing) are fully accessible and operational.",
        DecisionSeverity.SUCCESS
    ),
    BLOCKED_BY_PERMISSION(
        "FM blocked by permission",
        "Android Framework or SELinux policy blocks access to the BroadcastRadio service or hardware tuner device nodes.",
        DecisionSeverity.WARNING
    ),
    BLOCKED_BY_BINDER(
        "FM blocked by Binder",
        "FM Radio Binder service ('broadcastradio' or 'vendor.qti.hardware.fm') is unreachable or returned permission denied.",
        DecisionSeverity.ERROR
    ),
    BLOCKED_BY_HAL(
        "FM blocked by HAL",
        "BroadcastRadio HAL (HIDL/AIDL) daemon is not registered or failed to open session.",
        DecisionSeverity.ERROR
    ),
    BLOCKED_BY_DRIVER(
        "FM blocked by driver",
        "Qualcomm WCN399x / FastConnect FM radio transport driver (radio-iris / smd) is loaded but failed initialization or antenna check.",
        DecisionSeverity.ERROR
    ),
    BLOCKED_BY_KERNEL(
        "FM blocked by kernel",
        "Kernel device nodes (/dev/radio0, /dev/smd7) are missing or disabled in Samsung stock firmware kernel defconfig.",
        DecisionSeverity.CRITICAL
    ),
    HARDWARE_UNAVAILABLE(
        "FM hardware unavailable",
        "No RF tuner chipset detected on this hardware SKU or physical FM antenna connection is completely stripped.",
        DecisionSeverity.CRITICAL
    )
}

enum class DecisionSeverity { SUCCESS, WARNING, ERROR, CRITICAL }

enum class ProbeStatus { PASS, FAIL, RESTRICTED, NOT_FOUND, TESTING, PENDING }

data class LayerDiagnostic(
    val layerName: String,
    val targetComponent: String,
    val status: ProbeStatus,
    val details: String,
    val technicalPath: String
)

data class JniLibraryProbe(
    val libraryName: String,
    val expectedPath: String,
    val isFoundOnDisk: Boolean,
    val loadStatus: ProbeStatus,
    val exportedMethods: List<String>,
    val errorMessage: String? = null
)

data class BinderServiceProbe(
    val serviceName: String,
    val interfaceDescriptor: String,
    val isRunning: Boolean,
    val isReachable: Boolean,
    val permissionStatus: String,
    val transactionSupport: Boolean,
    val responseTimeMs: Long
)

data class HalInterfaceProbe(
    val halName: String,
    val version: String,
    val status: String, // Available, Inactive, Permission denied, Not registered
    val details: String
)

data class AudioRoutingProbe(
    val pathName: String,
    val isAvailable: Boolean,
    val currentRoute: String,
    val details: String
)

data class DeviceTargetInfo(
    val model: String,
    val device: String,
    val osVersion: String,
    val sdkInt: Int,
    val socModel: String,
    val hardware: String,
    val kernelInfo: String
)

data class CompleteEngineeringReport(
    val timestamp: String,
    val targetInfo: DeviceTargetInfo,
    val decision: ActivationDecision,
    val probabilityPercent: Int,
    val blockingComponent: String,
    val recommendedNextStep: String,
    val layerDiagnostics: List<LayerDiagnostic>,
    val jniProbes: List<JniLibraryProbe>,
    val binderProbes: List<BinderServiceProbe>,
    val halProbes: List<HalInterfaceProbe>,
    val audioProbes: List<AudioRoutingProbe>
)

data class FmStation(
    val frequency: Float, // e.g., 101.5
    val name: String,
    val genre: String,
    val rdsText: String,
    val isFavorite: Boolean = false
)
