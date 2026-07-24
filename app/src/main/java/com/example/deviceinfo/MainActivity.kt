package com.example.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExecutiveDeviceInfoApp()
        }
    }
}

@Composable
fun ExecutiveDeviceInfoApp() {
    val context = LocalContext.current
    var uptimeSeconds by remember { mutableLongStateOf(SystemClock.elapsedRealtime() / 1000) }

    // Live 1-second ticker loop for real-time uptime telemetry
    LaunchedEffect(Unit) {
        while (true) {
            uptimeSeconds = SystemClock.elapsedRealtime() / 1000
            delay(1000)
        }
    }

    val batteryInfo = remember(uptimeSeconds) { getBatteryInfo(context) }
    val ramInfo = remember(uptimeSeconds) { getRamInfo(context) }
    val storageInfo = remember { getStorageInfo() }
    val networkInfo = remember(uptimeSeconds) { getNetworkInfo(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090A0F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderSection()

            Spacer(modifier = Modifier.height(20.dp))
            UptimeLiveTickerCard(uptimeSeconds = uptimeSeconds)

            Spacer(modifier = Modifier.height(16.dp))
            CategoryHeader(title = "SYSTEM & HARDWARE MATRIX")
            
            GlassInfoCard(
                title = "SOC Architecture",
                items = listOf(
                    "Device Model" to "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (${Build.DEVICE})",
                    "SOC Hardware" to Build.HARDWARE,
                    "Board Name" to Build.BOARD,
                    "CPU Instruction Sets" to Build.SUPPORTED_ABIS.joinToString(", "),
                    "Active CPU Cores" to Runtime.getRuntime().availableProcessors().toString()
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            GlassInfoCard(
                title = "Software & Kernel",
                items = listOf(
                    "Android Version" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "Build ID" to Build.DISPLAY,
                    "Security Patch" to Build.VERSION.SECURITY_PATCH,
                    "Kernel Release" to System.getProperty("os.version").orEmpty(),
                    "Root Status" to if (checkRoot()) "Access Granted (Rooted)" else "Protected (Enforced)"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            CategoryHeader(title = "MEMORY & TELEMETRY")

            GlassInfoCard(
                title = "RAM Allocation",
                items = listOf(
                    "Total RAM" to ramInfo.first,
                    "Available Memory" to ramInfo.second,
                    "Memory Threshold" to ramInfo.third
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            GlassInfoCard(
                title = "Internal Storage",
                items = listOf(
                    "Total Internal" to storageInfo.first,
                    "Free Storage" to storageInfo.second
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            GlassInfoCard(
                title = "Power & Battery Node",
                items = listOf(
                    "Battery Level" to "${batteryInfo.level}%",
                    "Power Source" to batteryInfo.plugged,
                    "Battery Health" to batteryInfo.health,
                    "Voltage" to "${batteryInfo.voltage} mV",
                    "Temperature" to "${batteryInfo.temperature} °C"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            GlassInfoCard(
                title = "Network Interfaces",
                items = listOf(
                    "Connection Type" to networkInfo.first,
                    "Link Bandwidth" to networkInfo.second
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "EXECUTIVE TELEMETRY",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Device Matrix",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF0055FF))
                    )
                )
                .padding(1.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF0F111A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "I/O",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun UptimeLiveTickerCard(uptimeSeconds: Long) {
    val days = uptimeSeconds / 86400
    val hours = (uptimeSeconds % 86400) / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    val seconds = uptimeSeconds % 60

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF101422))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ACTIVE SYSTEM UPTIME",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds),
                color = Color(0xFF00E5FF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF8A94A6),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun GlassInfoCard(title: String, items: List<Pair<String, String>>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121520))
            .border(1.dp, Color(0xFF1E2435), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            items.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pair.first,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = pair.second,
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = Color(0xFF1A1F2C),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// System Data Extraction Helpers
private fun checkRoot(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )
    return paths.any { File(it).exists() }
}

private fun getRamInfo(context: Context): Triple<String, String, String> {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val totalGB = String.format("%.2f GB", memInfo.totalMem.toDouble() / (1024 * 1024 * 1024))
    val availGB = String.format("%.2f GB", memInfo.availMem.toDouble() / (1024 * 1024 * 1024))
    val threshGB = String.format("%.2f GB", memInfo.threshold.toDouble() / (1024 * 1024 * 1024))
    return Triple(totalGB, availGB, threshGB)
}

private fun getStorageInfo(): Pair<String, String> {
    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
    val totalGB = String.format("%.2f GB", totalBytes.toDouble() / (1024 * 1024 * 1024))
    val freeGB = String.format("%.2f GB", freeBytes.toDouble() / (1024 * 1024 * 1024))
    return Pair(totalGB, freeGB)
}

data class BatteryData(
    val level: Int,
    val plugged: String,
    val health: String,
    val voltage: Int,
    val temperature: Float
)

private fun getBatteryInfo(context: Context): BatteryData {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0

    val pluggedVal = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val pluggedStr = when (pluggedVal) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charge"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
        else -> "Discharging (Battery)"
    }

    val healthVal = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val healthStr = when (healthVal) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal / Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Critical Fail"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        else -> "Nominal"
    }

    val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

    return BatteryData(pct, pluggedStr, healthStr, voltage, temp)
}

private fun getNetworkInfo(context: Context): Pair<String, String> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return Pair("Offline / No Data", "0 Mbps")
    val caps = cm.getNetworkCapabilities(network) ?: return Pair("Offline", "0 Mbps")

    val type = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Interface"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Active Link"
    }

    val speedMbps = caps.linkDownstreamBandwidthKbps / 1000
    return Pair(type, "$speedMbps Mbps Downstream")
}
