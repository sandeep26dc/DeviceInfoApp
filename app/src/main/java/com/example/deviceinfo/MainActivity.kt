package com.example.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// --- DATA STRUCTURE FOR EXTRACTED HARDWARE DETAILS ---
data class DeviceSpec(
    val model: String,
    val manufacturer: String,
    val socName: String,
    val totalRamGb: Double,
    val availRamGb: Double,
    val ramUsagePercent: Int,
    val androidVersion: String,
    val apiLevel: Int,
    val batteryLevel: Int,
    val batteryStatusStr: String,
    val screenResolution: String,
    val screenDensityDpi: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PremiumDeviceDashboard()
            }
        }
    }
}

// --- HARDWARE EXTRACTION UTILITY ---
object DeviceExtractor {
    fun extractAll(context: Context): DeviceSpec {
        // RAM Metrics
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availRam = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val usedRam = totalRam - availRam
        val ramPercent = ((usedRam / totalRam) * 100).toInt()

        // Battery Metrics
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) ((level / scale.toFloat()) * 100).toInt() else 0

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL
        val batteryStatusText = if (isCharging) "Charging" else "Discharging"

        // Display Metrics
        val displayMetrics = context.resources.displayMetrics
        val res = "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}"

        return DeviceSpec(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            socName = Build.HARDWARE,
            totalRamGb = (totalRam * 10.0).roundToInt() / 10.0,
            availRamGb = (availRam * 10.0).roundToInt() / 10.0,
            ramUsagePercent = ramPercent,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            batteryLevel = batteryPct,
            batteryStatusStr = batteryStatusText,
            screenResolution = res,
            screenDensityDpi = displayMetrics.densityDpi
        )
    }
}

// --- GLASSMORPHIC CONTAINER COMPONENT ---
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassShape = RoundedCornerShape(cornerRadius)
    val glassBackground = Color(0xFF131822).copy(alpha = 0.75f)
    
    // Glowing neon border gradient
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF).copy(alpha = 0.45f),
            Color(0xFF7C4DFF).copy(alpha = 0.20f),
            Color.Transparent
        )
    )

    Column(
        modifier = modifier
            .clip(glassShape)
            .background(glassBackground)
            .border(width = 1.dp, brush = borderGradient, shape = glassShape)
            .padding(16.dp),
        content = content
    )
}

// --- MAIN EXECUTIVE DASHBOARD SCREEN ---
@Composable
fun PremiumDeviceDashboard() {
    val context = LocalContext.current
    var deviceData by remember { mutableStateOf<DeviceSpec?>(null) }
    var uptimeSeconds by remember { mutableLongStateOf(SystemClock.elapsedRealtime() / 1000) }

    // Instant Data Extraction on App Open
    LaunchedEffect(Unit) {
        deviceData = DeviceExtractor.extractAll(context)
        
        // Continuous Live Uptime Ticker
        while (true) {
            delay(1000)
            uptimeSeconds = SystemClock.elapsedRealtime() / 1000
        }
    }

    val darkGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B0D14), Color(0xFF040508))
    )

    val hours = uptimeSeconds / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    val seconds = uptimeSeconds % 60
    val uptimeFormatted = String.format("%02dh %02dm %02ds", hours, minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkGradient)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        deviceData?.let { spec ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM ARCHITECTURE",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "${spec.manufacturer.uppercase()} ${spec.model.uppercase()}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    // Live Status Pulsing Indicator
                    PulsingStatusBadge()
                }

                // Hero SOC & Uptime Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HARDWARE PLATFORM",
                                color = Color(0xFF8E9BAE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spec.socName.uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LIVE UPTIME",
                                color = Color(0xFF8E9BAE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uptimeFormatted,
                                color = Color(0xFF00E5FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Metric Grid Nodes
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        MetricArcTile(
                            title = "RAM ALLOCATION",
                            value = "${spec.availRamGb} GB",
                            subText = "Free of ${spec.totalRamGb} GB",
                            percentage = spec.ramUsagePercent,
                            accentColor = Color(0xFF00E5FF)
                        )
                    }
                    item {
                        MetricArcTile(
                            title = "BATTERY LEVEL",
                            value = "${spec.batteryLevel}%",
                            subText = spec.batteryStatusStr,
                            percentage = spec.batteryLevel,
                            accentColor = Color(0xFF7C4DFF)
                        )
                    }
                    item {
                        MetricGlassTile(
                            title = "ANDROID OS",
                            value = "Android ${spec.androidVersion}",
                            subText = "SDK API Level ${spec.apiLevel}"
                        )
                    }
                    item {
                        MetricGlassTile(
                            title = "DISPLAY PANEL",
                            value = spec.screenResolution,
                            subText = "${spec.screenDensityDpi} DPI Density"
                        )
                    }
                }
            }
        } ?: CircularProgressIndicator(
            color = Color(0xFF00E5FF),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// --- METRIC ARC TILE FOR GAUGES ---
@Composable
fun MetricArcTile(
    title: String,
    value: String,
    subText: String,
    percentage: Int,
    accentColor: Color
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = Color(0xFF8E9BAE), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subText, color = Color(0xFF5A6578), fontSize = 10.sp)
            }

            // Circular progress gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = (360f * (percentage / 100f)),
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$percentage%",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- STANDARD METRIC TILE ---
@Composable
fun MetricGlassTile(title: String, value: String, subText: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = Color(0xFF8E9BAE), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subText, color = Color(0xFF5A6578), fontSize = 10.sp)
    }
}

// --- PULSING STATUS BADGE ---
@Composable
fun PulsingStatusBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "ACTIVE",
            color = Color(0xFF00E5FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
