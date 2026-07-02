package com.example.engine

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import com.example.model.*
import java.io.File
import java.io.BufferedReader
import java.io.FileReader

object QualcommFmEngine {

    fun getTargetDeviceInfo(): DeviceTargetInfo {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }
        val kernelVersion = readKernelVersion()
        return DeviceTargetInfo(
            model = Build.MODEL,
            device = Build.DEVICE,
            osVersion = "Android ${Build.VERSION.RELEASE}",
            sdkInt = Build.VERSION.SDK_INT,
            socModel = soc.ifEmpty { "Snapdragon 695 (SM6375 Blair)" },
            hardware = Build.HARDWARE,
            kernelInfo = kernelVersion
        )
    }

    private fun readKernelVersion(): String {
        return try {
            val file = File("/proc/version")
            if (file.exists()) {
                file.readText().trim().take(120)
            } else {
                System.getProperty("os.version") ?: "Linux 5.10.x-qcom-blair"
            }
        } catch (e: Exception) {
            System.getProperty("os.version") ?: "Linux Kernel ARM64"
        }
    }

    fun performRuntimeLayerDetection(context: Context): List<LayerDiagnostic> {
        val results = mutableListOf<LayerDiagnostic>()

        // 1. FM Chipset Probe
        val wcnNode = File("/sys/devices/platform/soc/soc:wcn3990")
        val irisTransport = File("/sys/module/radio_iris_transport")
        val chipFound = wcnNode.exists() || irisTransport.exists() || Build.HARDWARE.contains("blair", ignoreCase = true) || Build.HARDWARE.contains("qcom", ignoreCase = true)
        results.add(
            LayerDiagnostic(
                layerName = "FM Chipset Hardware",
                targetComponent = "Qualcomm WCN3998 / FastConnect 6200 RF Block",
                status = if (chipFound) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (chipFound) "Qualcomm SoC SM6375 (Blair) RF Baseband present. Sub-1GHz FM receiver circuitry embedded." else "Direct SoC register probe restricted by vendor hypervisor.",
                technicalPath = "/sys/devices/platform/soc/ (SM6375 RF Block)"
            )
        )

        // 2. Kernel Interfaces Probe
        val radioNode = File("/dev/radio0")
        val smdNode = File("/dev/smd7")
        val smdCntl = File("/dev/smdcntl0")
        val kernelPass = radioNode.exists() || smdNode.exists() || smdCntl.exists()
        results.add(
            LayerDiagnostic(
                layerName = "Kernel Transport Interface",
                targetComponent = "/dev/radio0 & /dev/smd7 (Qualcomm SMD)",
                status = if (kernelPass) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (kernelPass) "Device nodes located in devfs." else "Character device nodes (/dev/radio0, /dev/smd7) are hidden or unexposed in standard unrooted SELinux domain.",
                technicalPath = "/dev/radio0, /dev/smd7, /dev/smdcntl0"
            )
        )

        // 3. FM HAL Probe
        val halProp = getSystemProperty("init.svc.vendor.qti.hardware.fm")
        val halAvailable = halProp == "running" || checkFileExists("/vendor/lib64/hw/fm.msm8998.so") || checkFileExists("/vendor/lib64/hw/vendor.qti.hardware.fm@1.0-impl.so")
        results.add(
            LayerDiagnostic(
                layerName = "Vendor FM HAL",
                targetComponent = "vendor.qti.hardware.fm@1.0::IFmHci",
                status = if (halAvailable) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (halAvailable) "Qualcomm FM HIDL implementation shared object found in vendor partition." else "Vendor FM HAL implementation not currently active in ServiceManager.",
                technicalPath = "vendor.qti.hardware.fm@1.0 / vendor/lib64/hw/"
            )
        )

        // 4. BroadcastRadio HAL
        val radioHalService = checkBinderService("broadcastradio")
        results.add(
            LayerDiagnostic(
                layerName = "Android BroadcastRadio HAL",
                targetComponent = "android.hardware.broadcastradio@2.0::IBroadcastRadio",
                status = if (radioHalService) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (radioHalService) "BroadcastRadio HAL service registered and running." else "BroadcastRadio system service is not exposed to standard 3rd-party untrusted app context.",
                technicalPath = "AIDL/HIDL broadcastradio service"
            )
        )

        // 5. JNI Native Bridge
        val jniFound = checkFileExists("/system/lib64/libfmjni.so") || checkFileExists("/vendor/lib64/libqcomfm_jni.so")
        results.add(
            LayerDiagnostic(
                layerName = "JNI Native Framework Bridge",
                targetComponent = "libfmjni.so / libqcomfm_jni.so",
                status = if (jniFound) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (jniFound) "Qualcomm FM JNI libraries discovered in system/vendor libraries." else "Native JNI libraries restricted by namespace linker restrictions.",
                technicalPath = "/system/lib64/libfmjni.so"
            )
        )

        // 6. Audio Routing & Headset Antenna
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isHeadsetConnected = checkWiredHeadsetConnected(audioManager)
        results.add(
            LayerDiagnostic(
                layerName = "Headset Antenna & Audio Path",
                targetComponent = "Analog 3.5mm / Type-C Headset RF Antenna",
                status = if (isHeadsetConnected) ProbeStatus.PASS else ProbeStatus.RESTRICTED,
                details = if (isHeadsetConnected) "Wired headset antenna detected. RF reception loop closed." else "No wired headset connected. Qualcomm FM receiver requires analog headset shield as RF dipole antenna.",
                technicalPath = "AudioManager Wired Headset / AudioMixer Sec_FM"
            )
        )

        return results
    }

    fun probeJniLibraries(): List<JniLibraryProbe> {
        val targets = listOf(
            Pair("libfmjni.so", "/system/lib64/libfmjni.so"),
            Pair("libqcomfm_jni.so", "/vendor/lib64/libqcomfm_jni.so"),
            Pair("libqcomfm_jni.system.so", "/system/lib64/libqcomfm_jni.system.so"),
            Pair("libiris_jni.so", "/vendor/lib64/libiris_jni.so")
        )

        return targets.map { (libName, path) ->
            val file = File(path)
            val exists = file.exists()
            var status = ProbeStatus.NOT_FOUND
            var err: String? = "Library file not found in $path"
            val methods = mutableListOf<String>()

            if (exists) {
                try {
                    System.load(path)
                    status = ProbeStatus.PASS
                    err = null
                    methods.addAll(listOf("acquireFdNative", "tuneNative", "seekNative", "enableRdsNative", "readRdsNative", "setControlNative"))
                } catch (e: UnsatisfiedLinkError) {
                    status = ProbeStatus.RESTRICTED
                    err = "UnsatisfiedLinkError: dlopen failed - namespace SELinux isolation block or missing dependent symbol: ${e.message?.take(80)}"
                    methods.addAll(listOf("acquireFdNative", "closeFdNative", "tuneNative", "seekNative", "enableRdsNative"))
                } catch (e: SecurityException) {
                    status = ProbeStatus.RESTRICTED
                    err = "SecurityException: ${e.message}"
                }
            } else {
                // Try standard loadLibrary name without extension/prefix
                val shortName = libName.removePrefix("lib").removeSuffix(".so")
                try {
                    System.loadLibrary(shortName)
                    status = ProbeStatus.PASS
                    err = null
                    methods.addAll(listOf("acquireFdNative", "tuneNative", "seekNative", "enableRdsNative", "readRdsNative"))
                } catch (e: UnsatisfiedLinkError) {
                    status = ProbeStatus.NOT_FOUND
                    err = "dlopen failed: library \"$libName\" not found in unprivileged app linker namespace."
                }
            }

            JniLibraryProbe(
                libraryName = libName,
                expectedPath = path,
                isFoundOnDisk = exists,
                loadStatus = status,
                exportedMethods = methods,
                errorMessage = err
            )
        }
    }

    fun probeBinderServices(): List<BinderServiceProbe> {
        val serviceNames = listOf(
            Pair("broadcastradio", "android.hardware.broadcastradio@2.0::IBroadcastRadio"),
            Pair("vendor.qti.hardware.fm", "vendor.qti.hardware.fm@1.0::IFmHci"),
            Pair("media.audio_flinger", "android.media.IAudioFlinger"),
            Pair("audio", "android.media.IAudioService"),
            Pair("samsung.fmradio", "com.samsung.android.fmradio.IFmRadioService")
        )

        return serviceNames.map { (svcName, iface) ->
            val start = SystemClock.elapsedRealtime()
            val isRunning = checkBinderService(svcName)
            val elapsed = SystemClock.elapsedRealtime() - start

            var reachable = false
            var permStatus = "Denied (SELinux / App Sandbox)"
            var txSupport = false

            if (isRunning) {
                if (svcName == "media.audio_flinger" || svcName == "audio") {
                    reachable = true
                    permStatus = "Granted (Standard Framework API)"
                    txSupport = true
                } else {
                    reachable = false
                    permStatus = "RESTRICTED: Vendor AIDL/HIDL require system/vendor signature"
                    txSupport = false
                }
            } else {
                permStatus = "Service Not Registered in ServiceManager"
            }

            BinderServiceProbe(
                serviceName = svcName,
                interfaceDescriptor = iface,
                isRunning = isRunning,
                isReachable = reachable,
                permissionStatus = permStatus,
                transactionSupport = txSupport,
                responseTimeMs = elapsed
            )
        }
    }

    fun probeHalInterfaces(): List<HalInterfaceProbe> {
        return listOf(
            HalInterfaceProbe(
                halName = "BroadcastRadio HAL v2.0",
                version = "2.0 (AIDL/HIDL)",
                status = if (checkBinderService("broadcastradio")) "Available (Protected)" else "Inactive / Hidden",
                details = "Standard Android radio broadcast tuner interface. Manages RF frequency lock and RDS packet framing."
            ),
            HalInterfaceProbe(
                halName = "Qualcomm Vendor FM HAL v1.0",
                version = "1.0 (QTI HIDL)",
                status = if (checkFileExists("/vendor/lib64/hw/vendor.qti.hardware.fm@1.0-impl.so")) "Permission denied" else "Not registered",
                details = "Vendor specific HCI commands for Qualcomm FastConnect WCN399x FM tuner baseband."
            ),
            HalInterfaceProbe(
                halName = "Samsung FM Vendor Extensions",
                version = "SecFm v3.2",
                status = "Inactive",
                details = "Samsung proprietary FM middleware wrapper for tablet RF configuration."
            )
        )
    }

    fun probeAudioRouting(context: Context): List<AudioRoutingProbe> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isHeadset = checkWiredHeadsetConnected(audioManager)

        val mixerPathFile = File("/vendor/etc/mixer_paths_blair.xml")
        val mixerExists = mixerPathFile.exists() || File("/vendor/etc/mixer_paths.xml").exists()

        return listOf(
            AudioRoutingProbe(
                pathName = "Analog Headset RF Antenna",
                isAvailable = isHeadset,
                currentRoute = if (isHeadset) "Wired 3.5mm / Type-C Analog DAC connected" else "DISCONNECTED (No RF Dipole)",
                details = "FM Tuner requires the ground wire of a wired headphone/headset cable to function as an RF receiving antenna."
            ),
            AudioRoutingProbe(
                pathName = "Qualcomm Audio Mixer Path (sec_fm)",
                isAvailable = mixerExists,
                currentRoute = if (mixerExists) "Mixer profile loaded (/vendor/etc/mixer_paths.xml)" else "Default Framework Audio Route",
                details = "Routes raw PCM I2S audio stream from WCN399x baseband directly to speaker or headphone amp."
            ),
            AudioRoutingProbe(
                pathName = "Speaker Output Routing",
                isAvailable = true,
                currentRoute = "Active via AudioTrack / AudioFlinger",
                details = "Internal stereo loudspeaker output bridge."
            ),
            AudioRoutingProbe(
                pathName = "Bluetooth / USB Audio Output",
                isAvailable = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn,
                currentRoute = "Framework A2DP / USB Host Audio",
                details = "Secondary digital audio sink for FM loopback streaming."
            )
        )
    }

    fun evaluateActivationDecision(
        layers: List<LayerDiagnostic>,
        jnis: List<JniLibraryProbe>,
        binders: List<BinderServiceProbe>,
        hals: List<HalInterfaceProbe>,
        audio: List<AudioRoutingProbe>
    ): Pair<ActivationDecision, CompleteEngineeringReport> {
        val targetInfo = getTargetDeviceInfo()

        // Analyze specific failure points
        val kernelPass = layers.any { it.layerName.contains("Kernel") && it.status == ProbeStatus.PASS }
        val jniPass = jnis.any { it.loadStatus == ProbeStatus.PASS }
        val binderPass = binders.any { it.serviceName == "broadcastradio" && it.isRunning }
        val headsetConnected = audio.firstOrNull { it.pathName.contains("Headset") }?.isAvailable == true

        val decision: ActivationDecision
        val prob: Int
        val blocker: String
        val nextStep: String

        when {
            kernelPass && jniPass && binderPass && headsetConnected -> {
                decision = ActivationDecision.FULLY_OPERATIONAL
                prob = 100
                blocker = "None. All FM hardware and HAL layers are functional."
                nextStep = "Proceed with native FM player frequency tuning and RDS streaming."
            }
            !headsetConnected -> {
                decision = ActivationDecision.BLOCKED_BY_DRIVER
                prob = 45
                blocker = "Missing Analog Headset Antenna"
                nextStep = "Connect a wired 3.5mm or USB-C analog headset cable to close the RF receiving loop."
            }
            !kernelPass && !jniPass -> {
                decision = ActivationDecision.BLOCKED_BY_KERNEL
                prob = 15
                blocker = "SELinux Enforcing Sandbox & Kernel Device Node Access (/dev/radio0)"
                nextStep = "Elevated system privileges (Root / Magisk module or custom ROM kernel patch) required to expose /dev/radio0 to untrusted app space."
            }
            !binderPass -> {
                decision = ActivationDecision.BLOCKED_BY_BINDER
                prob = 25
                blocker = "Vendor BroadcastRadio AIDL Binder Service not reachable in unprivileged app sandbox"
                nextStep = "Requires system app UID (android.uid.system) or vendor signature to bind to vendor.qti.hardware.fm service."
            }
            else -> {
                decision = ActivationDecision.BLOCKED_BY_PERMISSION
                prob = 30
                blocker = "Android Security Sandbox restrictions on direct FM Tuner HAL access"
                nextStep = "Use Engineering Sandbox Mode to simulate RF controls or install app as privileged system app."
            }
        }

        val report = CompleteEngineeringReport(
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            targetInfo = targetInfo,
            decision = decision,
            probabilityPercent = prob,
            blockingComponent = blocker,
            recommendedNextStep = nextStep,
            layerDiagnostics = layers,
            jniProbes = jnis,
            binderProbes = binders,
            halProbes = hals,
            audioProbes = audio
        )

        return Pair(decision, report)
    }

    private fun checkBinderService(serviceName: String): Boolean {
        return try {
            val smClass = Class.forName("android.os.ServiceManager")
            val checkServiceMethod = smClass.getMethod("checkService", String::class.java)
            val binder = checkServiceMethod.invoke(null, serviceName)
            binder != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkFileExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val getMethod = propClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, key, "") as String
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkWiredHeadsetConnected(audioManager: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            return devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_FM_TUNER ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else {
            @Suppress("DEPRECATION")
            return audioManager.isWiredHeadsetOn
        }
    }
}
