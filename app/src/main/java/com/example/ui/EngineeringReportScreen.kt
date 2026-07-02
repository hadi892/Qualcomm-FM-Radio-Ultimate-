package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun EngineeringReportScreen(
    report: CompleteEngineeringReport?
) {
    val context = LocalContext.current

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val reportMarkdown = generateMarkdownReport(report)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                text = "STEP 8: COMPLETE ENGINEERING REPORT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generated on ${report.timestamp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Qualcomm FM Diagnostic Report", reportMarkdown)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Engineering Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Log")
                    }
                }
            }
        }

        item {
            ExecutiveSummaryCard(report)
        }

        item {
            Text(
                text = "RAW DIAGNOSTIC TELEMETRY TRANSCRIPT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = reportMarkdown,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ExecutiveSummaryCard(report: CompleteEngineeringReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "EXECUTIVE DIAGNOSTIC SUMMARY",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            SummaryRow(label = "Target Device", value = "${report.targetInfo.model} (${report.targetInfo.device})")
            SummaryRow(label = "SoC Platform", value = report.targetInfo.socModel)
            SummaryRow(label = "OS Version", value = "${report.targetInfo.osVersion} (SDK ${report.targetInfo.sdkInt})")
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow(label = "Activation Decision", value = report.decision.title, isBold = true, valueColor = MaterialTheme.colorScheme.primary)
            SummaryRow(label = "Activation Probability", value = "${report.probabilityPercent}% (in unrooted 3rd-party app sandbox)")
            SummaryRow(label = "Blocking Component", value = report.blockingComponent)
            SummaryRow(label = "Recommended Next Step", value = report.recommendedNextStep)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun generateMarkdownReport(r: CompleteEngineeringReport): String {
    val sb = StringBuilder()
    sb.appendLine("====================================================")
    sb.appendLine("QUALCOMM FM RADIO ACTIVATION ENGINE ULTIMATE")
    sb.appendLine("PRODUCTION DIAGNOSTIC REPORT")
    sb.appendLine("Timestamp: ${r.timestamp}")
    sb.appendLine("====================================================")
    sb.appendLine()
    sb.appendLine("[TARGET DEVICE METADATA]")
    sb.appendLine("Model: ${r.targetInfo.model}")
    sb.appendLine("Device Code: ${r.targetInfo.device}")
    sb.appendLine("Android OS: ${r.targetInfo.osVersion} (SDK ${r.targetInfo.sdkInt})")
    sb.appendLine("Qualcomm SoC: ${r.targetInfo.socModel}")
    sb.appendLine("Hardware: ${r.targetInfo.hardware}")
    sb.appendLine("Kernel: ${r.targetInfo.kernelInfo}")
    sb.appendLine()
    sb.appendLine("[FINAL ACTIVATION DECISION]")
    sb.appendLine("Result: ${r.decision.title}")
    sb.appendLine("Probability: ${r.probabilityPercent}%")
    sb.appendLine("Blocking Component: ${r.blockingComponent}")
    sb.appendLine("Recommended Action: ${r.recommendedNextStep}")
    sb.appendLine()
    sb.appendLine("[STEP 1: RUNTIME LAYER DETECTION]")
    r.layerDiagnostics.forEach { l ->
        sb.appendLine("- [${l.status.name}] ${l.layerName}: ${l.details}")
    }
    sb.appendLine()
    sb.appendLine("[STEP 2: JNI NATIVE LIBRARY PROBES]")
    r.jniProbes.forEach { j ->
        sb.appendLine("- [${j.loadStatus.name}] ${j.libraryName} @ ${j.expectedPath}")
        if (j.errorMessage != null) sb.appendLine("  Error: ${j.errorMessage}")
        if (j.exportedMethods.isNotEmpty()) sb.appendLine("  Symbols: ${j.exportedMethods.joinToString(", ")}")
    }
    sb.appendLine()
    sb.appendLine("[STEP 3: BINDER SERVICE PROBES]")
    r.binderProbes.forEach { b ->
        sb.appendLine("- [${if (b.isRunning) "RUNNING" else "INACTIVE"}] ${b.serviceName} (${b.interfaceDescriptor})")
        sb.appendLine("  Permission: ${b.permissionStatus} | Reachable: ${b.isReachable}")
    }
    sb.appendLine()
    sb.appendLine("[STEP 4: HAL INTERFACE PROBES]")
    r.halProbes.forEach { h ->
        sb.appendLine("- ${h.halName} (${h.version}): ${h.status}")
    }
    sb.appendLine()
    sb.appendLine("[STEP 5: AUDIO ROUTING PROBES]")
    r.audioProbes.forEach { a ->
        sb.appendLine("- [${if (a.isAvailable) "ACTIVE" else "UNAVAILABLE"}] ${a.pathName}: ${a.currentRoute}")
    }
    sb.appendLine()
    sb.appendLine("====================================================")
    sb.appendLine("END OF ENGINEERING REPORT")
    return sb.toString()
}
