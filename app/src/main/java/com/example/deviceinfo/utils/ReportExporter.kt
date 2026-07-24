package com.example.deviceinfo.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

data class CompleteDeviceSpec(
    val manufacturer: String = "",
    val model: String = "",
    val socName: String = "",
    val availRamGb: String = "",
    val totalRamGb: String = "",
    val freeStorageGb: String = "",
    val totalStorageGb: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val batteryLevel: Int = 0,
    val batteryStatusStr: String = "",
    val screenResolution: String = "",
    val screenDensityDpi: Int = 0
)

object ReportExporter {
    fun shareDeviceReport(context: Context, spec: CompleteDeviceSpec) {
        val reportText = """
            --- DEVICE INFORMATION REPORT ---
            Device: ${spec.manufacturer} ${spec.model}
            SoC / CPU: ${spec.socName}
            RAM: ${spec.availRamGb} available / ${spec.totalRamGb} total
            Storage: ${spec.freeStorageGb} free / ${spec.totalStorageGb} total
            OS: Android ${spec.androidVersion} (API ${spec.apiLevel})
            Battery: ${spec.batteryLevel}% (${spec.batteryStatusStr})
            Display: ${spec.screenResolution} (${spec.screenDensityDpi} DPI)
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Device Specs Report")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Device Report"))
    }
}
