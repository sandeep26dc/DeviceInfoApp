package com.example.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    onSurface = Color(0xFFF8FAFC)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeviceInfoScreen(context = this)
                }
            }
        }
    }
}

@Composable
fun DeviceInfoScreen(context: Context) {
    val deviceSpecs = getDeviceSpecs(context)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection()
        }

        deviceSpecs.forEach { category ->
            item {
                InfoCategoryCard(category = category)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Device Info Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )
        Text(
            text = "Complete Hardware & System Specifications",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun InfoCategoryCard(category: InfoCategory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = category.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF818CF8),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            category.items.forEachIndexed { index, item ->
                InfoRow(label = item.label, value = item.value)
                if (index < category.items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF334155))
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF8FAFC),
            modifier = Modifier.weight(1.2f)
        )
    }
}

data class InfoItem(val label: String, val value: String)
data class InfoCategory(val title: String, val items: List<InfoItem>)

fun getDeviceSpecs(context: Context): List<InfoCategory> {
    // 1. Identity Category
    val identityItems = listOf(
        InfoItem("Brand", Build.BRAND.replaceFirstChar { it.uppercase() }),
        InfoItem("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }),
        InfoItem("Model", Build.MODEL),
        InfoItem("Device / Board", "${Build.DEVICE} (${Build.BOARD})"),
        InfoItem("Hardware", Build.HARDWARE),
        InfoItem("IMEI Status", getImeiStatus(context))
    )

    // 2. Android OS Category
    val osItems = listOf(
        InfoItem("Android Version", Build.VERSION.RELEASE),
        InfoItem("API Level", Build.VERSION.SDK_INT.toString()),
        InfoItem("Security Patch", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
        InfoItem("Build ID", Build.DISPLAY),
        InfoItem("Fingerprint", Build.FINGERPRINT.take(30) + "...")
    )

    // 3. Processor & CPU
    val cpuItems = listOf(
        InfoItem("CPU Architecture", System.getProperty("os.arch") ?: "Unknown"),
        InfoItem("CPU Cores", Runtime.getRuntime().availableProcessors().toString()),
        InfoItem("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
    )

    // 4. Memory & Storage
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)

    val totalRamGb = String.format("%.2f GB", memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
    val availRamGb = String.format("%.2f GB", memInfo.availMem / (1024.0 * 1024.0 * 1024.0))

    val stat = StatFs(Environment.getDataDirectory().path)
    val totalStorageGb = String.format("%.2f GB", (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0))
    val freeStorageGb = String.format("%.2f GB", (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0))

    val memoryItems = listOf(
        InfoItem("Total RAM", totalRamGb),
        InfoItem("Available RAM", availRamGb),
        InfoItem("Internal Storage", totalStorageGb),
        InfoItem("Free Storage", freeStorageGb)
    )

    // 5. Display Info
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    wm.defaultDisplay.getMetrics(metrics)

    val displayItems = listOf(
        InfoItem("Resolution", "${metrics.widthPixels} x ${metrics.heightPixels} px"),
        InfoItem("Density (DPI)", "${metrics.densityDpi} dpi"),
        InfoItem("Screen Density", "${metrics.density}x")
    )

    // 6. Battery Info
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
        context.registerReceiver(null, filter)
    }
    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0

    val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
    val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

    val batteryItems = listOf(
        InfoItem("Battery Level", "$batteryPct%"),
        InfoItem("Charging Status", if (isCharging) "Charging" else "Discharging"),
        InfoItem("Technology", tech)
    )

    return listOf(
        InfoCategory("Device & Identity", identityItems),
        InfoCategory("Operating System", osItems),
        InfoCategory("Processor & CPU", cpuItems),
        InfoCategory("RAM & Storage", memoryItems),
        InfoCategory("Display Specs", displayItems),
        InfoCategory("Battery Health", batteryItems)
    )
}

fun getImeiStatus(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "Restricted (Android 10+ Security Policy)"
    } else {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @Suppress("DEPRECATION")
            tm.deviceId ?: "Not Available"
        } catch (e: Exception) {
            "Permission Denied"
        }
    }
}
