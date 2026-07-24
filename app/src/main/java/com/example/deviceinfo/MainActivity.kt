package com.example.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

// --- DATA STRUCTURES ---
data class CompleteDeviceSpec(
    val model: String,
    val manufacturer: String,
    val socName: String,
    val board: String,
    val totalRamGb: Double,
    val availRamGb: Double,
    val ramUsagePercent: Int,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val storageUsagePercent: Int,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val securityPatch: String,
    val batteryLevel: Int,
    val batteryStatusStr: String,
    val batteryHealthStr: String,
    val batteryTemp: Float,
    val batteryVoltage: Int,
    val screenResolution: String,
    val screenDensityDpi: Int,
    val activeCores: Int,
    val networkType: String,
    val isRooted: Boolean
)

enum class NavigationTab(val title: String) {
    HARDWARE("Hardware"),
    TELEMETRY("Telemetry"),
    SENSORS("Sensors")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ExecutiveDeviceApp()
            }
        }
    }
}

// --- HARDWARE EXTRACTION UTILITY ---
object FullDeviceExtractor {
    fun extractAll(context: Context): CompleteDeviceSpec {
        // RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availRam = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val ramPercent = (((totalRam - availRam) / totalRam) * 100).toInt()

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
        val freeStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalStorage = totalStorageBytes / (1024.0 * 1024.0 * 1024.0)
        val freeStorage = freeStorageBytes / (1024.0 * 1024.0 * 1024.0)
        val storagePercent = (((totalStorage - freeStorage) / totalStorage) * 100).toInt()

        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) ((level / scale.toFloat()) * 100).toInt() else 0

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING || statusInt == BatteryManager.BATTERY_STATUS_FULL
        val batteryStatusText = if (isCharging) "Charging (AC/USB)" else "Discharging"

        val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal Health"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            else -> "Nominal"
        }
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        // Display
        val dm = context.resources.displayMetrics
        val res = "${dm.widthPixels} x ${dm.heightPixels}"

        // Network
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val networkText = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi High-Speed"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 5G/LTE"
            else -> "Offline / Local"
        }

        // Root check
        val rootPaths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su")
        val isRooted = rootPaths.any { File(it).exists() }

        return CompleteDeviceSpec(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            socName = Build.HARDWARE,
            board = Build.BOARD,
            totalRamGb = (totalRam * 10.0).roundToInt() / 10.0,
            availRamGb = (availRam * 10.0).roundToInt() / 10.0,
            ramUsagePercent = ramPercent,
            totalStorageGb = (totalStorage * 10.0).roundToInt() / 10.0,
            freeStorageGb = (freeStorage * 10.0).roundToInt() / 10.0,
            storageUsagePercent = storagePercent,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.DISPLAY,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            batteryLevel = batteryPct,
            batteryStatusStr = batteryStatusText,
            batteryHealthStr = healthText,
            batteryTemp = temp,
            batteryVoltage = voltage,
            screenResolution = res,
            screenDensityDpi = dm.densityDpi,
            activeCores = Runtime.getRuntime().availableProcessors(),
            networkType = networkText,
            isRooted = isRooted
        )
    }
}

// --- MAIN SCREEN DASHBOARD ---
@Composable
fun ExecutiveDeviceApp() {
    val context = LocalContext.current
    var deviceSpec by remember { mutableStateOf<CompleteDeviceSpec?>(null) }
    var activeTab by remember { mutableStateOf(NavigationTab.HARDWARE) }
    var uptimeSeconds by remember { mutableLongStateOf(SystemClock.elapsedRealtime() / 1000) }

    LaunchedEffect(Unit) {
        deviceSpec = FullDeviceExtractor.extractAll(context)
        while (true) {
            delay(1000)
            uptimeSeconds = SystemClock.elapsedRealtime() / 1000
        }
    }

    val darkBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B0D14), Color(0xFF030406))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        deviceSpec?.let { spec ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Section
                HeaderView(
                    manufacturer = spec.manufacturer,
                    model = spec.model
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hero Uptime Banner Card
                UptimeHeroCard(uptimeSeconds = uptimeSeconds)

                Spacer(modifier = Modifier.height(16.dp))

                // Glassmorphic Tab Bar Navigation
                GlassTabBar(
                    selectedTab = activeTab,
                    onTabSelected = { activeTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content Switcher
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        NavigationTab.HARDWARE -> HardwareTabContent(spec)
                        NavigationTab.TELEMETRY -> TelemetryTabContent(spec)
                        NavigationTab.SENSORS -> SensorsTabContent(context)
                    }
                }
            }
        } ?: CircularProgressIndicator(
            color = Color(0xFF00E5FF),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// --- COMPONENT VIEWS ---

@Composable
fun HeaderView(manufacturer: String, model: String) {
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
                text = "${manufacturer.uppercase()} ${model.uppercase()}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ONLINE",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun UptimeHeroCard(uptimeSeconds: Long) {
    val days = uptimeSeconds / 86400
    val hours = (uptimeSeconds % 86400) / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    val seconds = uptimeSeconds % 60
    val uptimeFormatted = String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds)

    ExecutiveGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE SYSTEM UPTIME TICKER",
                    color = Color(0xFF8E9BAE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uptimeFormatted,
                    color = Color(0xFF00E5FF),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            PulsingNode()
        }
    }
}

@Composable
fun GlassTabBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101420))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NavigationTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val bgAlpha by animateFloatAsState(if (isSelected) 0.25f else 0.0f, label = "tabBg")
            val textColor = if (isSelected) Color(0xFF00E5FF) else Color.Gray

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = bgAlpha))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title.uppercase(),
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// --- TAB CONTENTS ---

@Composable
fun HardwareTabContent(spec: CompleteDeviceSpec) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GaugeMetricRow(
                title1 = "RAM ALLOCATION",
                val1 = "${spec.availRamGb} GB",
                sub1 = "Free of ${spec.totalRamGb} GB",
                pct1 = spec.ramUsagePercent,
                color1 = Color(0xFF00E5FF),
                title2 = "INTERNAL STORAGE",
                val2 = "${spec.freeStorageGb} GB",
                sub2 = "Free of ${spec.totalStorageGb} GB",
                pct2 = spec.storageUsagePercent,
                color2 = Color(0xFF7C4DFF)
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Processor & Core Cluster",
                items = listOf(
                    "SOC Hardware" to spec.socName.uppercase(),
                    "Board Name" to spec.board,
                    "Active CPU Cores" to "${spec.activeCores} Cores Active",
                    "Root Privilege" to if (spec.isRooted) "Granted (Rooted)" else "Enforced (Protected)"
                )
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Display Panel Specification",
                items = listOf(
                    "Native Resolution" to spec.screenResolution,
                    "Pixel Density" to "${spec.screenDensityDpi} DPI"
                )
            )
        }
    }
}

@Composable
fun TelemetryTabContent(spec: CompleteDeviceSpec) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GaugeMetricRow(
                title1 = "BATTERY LEVEL",
                val1 = "${spec.batteryLevel}%",
                sub1 = spec.batteryStatusStr,
                pct1 = spec.batteryLevel,
                color1 = Color(0xFF00E676),
                title2 = "POWER NODE",
                val2 = "${spec.batteryTemp}°C",
                sub2 = "${spec.batteryVoltage} mV",
                pct2 = ((spec.batteryTemp / 60f) * 100).toInt().coerceIn(0, 100),
                color2 = Color(0xFFFF9100)
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Software & System Build",
                items = listOf(
                    "Android Version" to "Android ${spec.androidVersion} (API ${spec.apiLevel})",
                    "Build Display ID" to spec.buildId,
                    "Security Patch" to spec.securityPatch,
                    "Network State" to spec.networkType
                )
            )
        }
    }
}

@Composable
fun SensorsTabContent(context: Context) {
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensors = remember { sensorManager.getSensorList(Sensor.TYPE_ALL) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = "DETECTED HARDWARE SENSORS (${sensors.size})",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(sensors.size) { index ->
            val sensor = sensors[index]
            ExecutiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sensor.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vendor: ${sensor.vendor}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Power: ${sensor.power} mA",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- HELPER GLASS CARDS & GAUGES ---

@Composable
fun ExecutiveGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassShape = RoundedCornerShape(18.dp)
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF).copy(alpha = 0.35f),
            Color(0xFF7C4DFF).copy(alpha = 0.15f),
            Color.Transparent
        )
    )

    Column(
        modifier = modifier
            .clip(glassShape)
            .background(Color(0xFF11141F).copy(alpha = 0.85f))
            .border(1.dp, borderGradient, glassShape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ExecutiveInfoCard(title: String, items: List<Pair<String, String>>) {
    ExecutiveGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                Text(text = pair.first, color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = pair.second,
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (index < items.size - 1) {
                HorizontalDivider(color = Color(0xFF1E2435), thickness = 0.8.dp)
            }
        }
    }
}

@Composable
fun GaugeMetricRow(
    title1: String, val1: String, sub1: String, pct1: Int, color1: Color,
    title2: String, val2: String, sub2: String, pct2: Int, color2: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExecutiveGaugeCard(
            title = title1, value = val1, subText = sub1, percentage = pct1, accentColor = color1,
            modifier = Modifier.weight(1f)
        )
        ExecutiveGaugeCard(
            title = title2, value = val2, subText = sub2, percentage = pct2, accentColor = color2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ExecutiveGaugeCard(
    title: String, value: String, subText: String, percentage: Int, accentColor: Color, modifier: Modifier = Modifier
) {
    ExecutiveGlassCard(modifier = modifier) {
        Text(text = title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = subText, color = Color.Gray, fontSize = 10.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = (360f * (percentage / 100f)),
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(text = "$percentage%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PulsingNode() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF00E5FF).copy(alpha = alpha))
    )
}
