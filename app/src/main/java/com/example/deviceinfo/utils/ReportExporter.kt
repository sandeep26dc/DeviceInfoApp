package com.example.deviceinfo.utils

import android.content.Context
import android.content.Intent
import com.example.deviceinfo.CompleteDeviceSpec

object ReportExporter {
    fun shareSpecReport(context: Context, spec: CompleteDeviceSpec) {
        val reportText = """
            ====================================
            SYSTEM DIAGNOSTIC REPORT
            ====================================
            Device: ${spec.manufacturer} ${spec.model}
            SoC: ${spec.socName}
            RAM: ${spec.availRamGb} GB free / ${spec.totalRamGb} GB total
            Storage: ${spec.freeStorageGb} GB free / ${spec.totalStorageGb} GB total
            OS: Android ${spec.androidVersion} (API ${spec.apiLevel})
            Battery: ${spec.batteryLevel}% (${spec.batteryStatusStr})
            Display: ${spec.screenResolution} (${spec.screenDensityDpi} DPI)
            Root Status: ${if (spec.isRooted) "Rooted" else "Not Rooted"}
            ====================================
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Device Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Report Via"))
    }
}
