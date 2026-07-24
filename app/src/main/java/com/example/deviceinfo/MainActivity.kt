package com.example.deviceinfo

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

// --- DATA STRUCTURES ---

data class CpuCoreInfo(
    val coreIndex: Int,
    val curFreqMhz: Long,
    val maxFreqMhz: Long
)

data class CameraSpecInfo(
    val id: String,
    val facing: String,
    val megapixels: String,
    val sensorOrientation: Int,
    val hasFlash: Boolean
)

data class WifiSpecInfo(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    val is5GHzSupported: Boolean,
    val is6GHzSupported: Boolean,
    val linkSpeedMbps: Int
)

data class BluetoothSpecInfo(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    val leSupported: Boolean
)

data class ComprehensiveDeviceSpec(
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
    val kernelVersion: String,
    val batteryLevel: Int,
    val batteryStatusStr: String,
    val batteryHealthStr: String,
    val batteryTemp: Float,
    val batteryVoltage: Int,
    val screenResolution: String,
    val screenDensityDpi: Int,
    val activeCores: Int,
    val networkType: String,
    val isRooted: Boolean,
    val cameras: List<CameraSpecInfo>,
    val wifiInfo: WifiSpecInfo,
    val bluetoothInfo: BluetoothSpecInfo
)

enum class NavigationTab(val title: String) {
    HARDWARE("Hardware"),
    CPU_CORES("CPU Nodes"),
    CONNECTIVITY("Wireless"),
    CAMERA("Cameras"),
    SOFTWARE("System OS"),
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

// --- HARDWARE & SYSTEM EXTRACTION UTILITY ---

object ComprehensiveExtractor {

    fun readCpuFrequencies(): List<CpuCoreInfo> {
        val numCores = Runtime.getRuntime().availableProcessors()
        val coreList = mutableListOf<CpuCoreInfo>()

        for (i in 0 until numCores) {
            var curFreq = -1L
            var maxFreq = -1L

            runCatching {
                val readerCur = RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq", "r")
                curFreq = readerCur.readLine().toLong() / 1000
                readerCur.close()
            }

            runCatching {
                val readerMax = RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq", "r")
                maxFreq = readerMax.readLine().toLong() / 1000
                readerMax.close()
            }

            coreList.add(
                CpuCoreInfo(
                    coreIndex = i,
                    curFreqMhz = if (curFreq > 0) curFreq else 0L,
                    maxFreqMhz = if (maxFreq > 0) maxFreq else 0L
                )
            )
        }
        return coreList
    }

    fun extractCameras(context: Context): List<CameraSpecInfo> {
        val list = mutableListOf<CameraSpecInfo>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return list

        runCatching {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front Facing"
                    CameraCharacteristics.LENS_FACING_BACK -> "Rear Main"
                    else -> "External / Secondary"
                }

                val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val megapixels = if (sensorSize != null) {
                    val mp = (sensorSize.width * sensorSize.height) / 1_000_000.0
                    String.format("%.1f MP (%dx%d)", mp, sensorSize.width, sensorSize.height)
                } else "Unknown"

                val orientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                list.add(
                    CameraSpecInfo(
                        id = id,
                        facing = facingStr,
                        megapixels = megapixels,
                        sensorOrientation = orientation,
                        hasFlash = flashAvailable
                    )
                )
            }
        }
        return list
    }

    fun extractWifi(context: Context): WifiSpecInfo {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            return WifiSpecInfo(isSupported = false, isEnabled = false, is5GHzSupported = false, is6GHzSupported = false, linkSpeedMbps = 0)
        }

        val is5G = wifiManager.is5GHzBandSupported
        val is6G = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wifiManager.is6GHzBandSupported else false
        val speed = wifiManager.connectionInfo?.linkSpeed ?: 0

        return WifiSpecInfo(
            isSupported = true,
            isEnabled = wifiManager.isWifiEnabled,
            is5GHzSupported = is5G,
            is6GHzSupported = is6G,
            linkSpeedMbps = speed
        )
    }

    fun extractBluetooth(context: Context): BluetoothSpecInfo {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter

        if (adapter == null) {
            return BluetoothSpecInfo(isSupported = false, isEnabled = false, leSupported = false)
        }

        val hasLe = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)

        return BluetoothSpecInfo(
            isSupported = true,
            isEnabled = adapter.isEnabled,
            leSupported = hasLe
        )
    }

    fun extractAll(context: Context): ComprehensiveDeviceSpec {
        // RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availRam = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        val ramPercent = (((totalRam - availRam) / totalRam) * 100).toInt()

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
        val freeStorage = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
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
        val batteryStatusText = if (isCharging) "Charging (Active)" else "Discharging"

        val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal Good"
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
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi High-Speed Network"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular LTE / 5G Mobile"
            else -> "Disconnected / Offline"
        }

        // Root
        val rootPaths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su")
        val isRooted = rootPaths.any { File(it).exists() }

        // Kernel
        val kernel = System.getProperty("os.version") ?: "Linux Unknown"

        return ComprehensiveDeviceSpec(
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
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            kernelVersion = kernel,
            batteryLevel = batteryPct,
            batteryStatusStr = batteryStatusText,
            batteryHealthStr = healthText,
            batteryTemp = temp,
            batteryVoltage = voltage,
            screenResolution = res,
            screenDensityDpi = dm.densityDpi,
            activeCores = Runtime.getRuntime().availableProcessors(),
            networkType = networkText,
            isRooted = isRooted,
            cameras = extractCameras(context),
            wifiInfo = extractWifi(context),
            bluetoothInfo = extractBluetooth(context)
        )
    }
}

// --- MAIN EXECUTIVE DASHBOARD SCREEN ---

@Composable
fun ExecutiveDeviceApp() {
    val context = LocalContext.current
    var deviceSpec by remember { mutableStateOf<ComprehensiveDeviceSpec?>(null) }
    var cpuCores by remember { mutableStateOf<List<CpuCoreInfo>>(emptyList()) }
    var activeTab by remember { mutableStateOf(NavigationTab.HARDWARE) }
    var uptimeSeconds by remember { mutableLongStateOf(SystemClock.elapsedRealtime() / 1000) }

    // Instant initial load & real-time core monitoring loops
    LaunchedEffect(Unit) {
        deviceSpec = ComprehensiveExtractor.extractAll(context)
        while (true) {
            cpuCores = ComprehensiveExtractor.readCpuFrequencies()
            uptimeSeconds = SystemClock.elapsedRealtime() / 1000
            delay(1000)
        }
    }

    val darkBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0C14), Color(0xFF030406))
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
                HeaderView(manufacturer = spec.manufacturer, model = spec.model)

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Uptime Banner Card
                UptimeHeroCard(uptimeSeconds = uptimeSeconds)

                Spacer(modifier = Modifier.height(12.dp))

                // Glassmorphic Tab Navigation
                GlassTabBar(selectedTab = activeTab, onTabSelected = { activeTab = it })

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content Switcher
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        NavigationTab.HARDWARE -> HardwareTabContent(spec)
                        NavigationTab.CPU_CORES -> CpuCoresTabContent(cpuCores)
                        NavigationTab.CONNECTIVITY -> ConnectivityTabContent(spec)
                        NavigationTab.CAMERA -> CameraTabContent(spec.cameras)
                        NavigationTab.SOFTWARE -> SoftwareTabContent(spec)
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

// --- HEADER & NAVIGATION ---

@Composable
fun HeaderView(manufacturer: String, model: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "EXECUTIVE DIAGNOSTICS",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "${manufacturer.uppercase()} ${model.uppercase()}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        PulsingBadge()
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
                    text = "ACTIVE SYSTEM UPTIME",
                    color = Color(0xFF8E9BAE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uptimeFormatted,
                    color = Color(0xFF00E5FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "LIVE TICKER",
                color = Color(0xFF7C4DFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GlassTabBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(NavigationTab.values().size) { index ->
            val tab = NavigationTab.values()[index]
            val isSelected = selectedTab == tab
            val bgAlpha by animateFloatAsState(if (isSelected) 0.30f else 0.05f, label = "tabBg")
            val textColor = if (isSelected) Color(0xFF00E5FF) else Color.Gray

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = bgAlpha))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFF1E2638),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title.uppercase(),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// --- TAB CONTENT IMPLEMENTATIONS ---

@Composable
fun HardwareTabContent(spec: ComprehensiveDeviceSpec) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GaugeMetricRow(
                title1 = "RAM ALLOCATION",
                val1 = "${spec.availRamGb} GB",
                sub1 = "Free of ${spec.totalRamGb} GB",
                pct1 = spec.ramUsagePercent,
                color1 = Color(0xFF00E5FF),
                title2 = "STORAGE MEMORY",
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
                    "Total Cores" to "${spec.activeCores} CPU Cores",
                    "Root Access" to if (spec.isRooted) "Detected (Rooted)" else "Enforced (Protected)"
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
fun CpuCoresTabContent(cores: List<CpuCoreInfo>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = "REAL-TIME CPU CLUSTER FREQUENCIES (${cores.size} CORES)",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(cores.size) { index ->
            val core = cores[index]
            val pct = if (core.maxFreqMhz > 0) ((core.curFreqMhz.toFloat() / core.maxFreqMhz) * 100).toInt() else 0

            ExecutiveGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CPU Core #${core.coreIndex}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (core.maxFreqMhz > 0) "Max Cap: ${core.maxFreqMhz} MHz" else "Offline / Scaled Down",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (core.curFreqMhz > 0) "${core.curFreqMhz} MHz" else "Idle",
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Mini Linear Progress Indicator
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivityTabContent(spec: ComprehensiveDeviceSpec) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ExecutiveInfoCard(
                title = "Wi-Fi Hardware Node",
                items = listOf(
                    "Hardware Supported" to if (spec.wifiInfo.isSupported) "Present" else "Absent",
                    "Radio State" to if (spec.wifiInfo.isEnabled) "Enabled & Active" else "Disabled",
                    "5 GHz Band Support" to if (spec.wifiInfo.is5GHzSupported) "Supported" else "Not Supported",
                    "6 GHz Wi-Fi 6E/7" to if (spec.wifiInfo.is6GHzSupported) "Supported" else "Not Supported",
                    "Current Link Speed" to "${spec.wifiInfo.linkSpeedMbps} Mbps"
                )
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Bluetooth Hardware Node",
                items = listOf(
                    "Hardware Adapter" to if (spec.bluetoothInfo.isSupported) "Present" else "Absent",
                    "Radio Power State" to if (spec.bluetoothInfo.isEnabled) "ON" else "OFF",
                    "Bluetooth LE (Low Energy)" to if (spec.bluetoothInfo.leSupported) "Supported" else "Not Supported"
                )
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Network Connection State",
                items = listOf(
                    "Active Interface" to spec.networkType
                )
            )
        }
    }
}

@Composable
fun CameraTabContent(cameras: List<CameraSpecInfo>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "DETECTED CAMERA SENSORS (${cameras.size})",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(cameras.size) { index ->
            val cam = cameras[index]
            ExecutiveInfoCard(
                title = "Camera ID: ${cam.id} (${cam.facing})",
                items = listOf(
                    "Resolution" to cam.megapixels,
                    "Sensor Orientation" to "${cam.sensorOrientation}°",
                    "Flash Hardware" to if (cam.hasFlash) "Available" else "Not Available"
                )
            )
        }
    }
}

@Composable
fun SoftwareTabContent(spec: ComprehensiveDeviceSpec) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GaugeMetricRow(
                title1 = "BATTERY LEVEL",
                val1 = "${spec.batteryLevel}%",
                sub1 = spec.batteryStatusStr,
                pct1 = spec.batteryLevel,
                color1 = Color(0xFF00E676),
                title2 = "THERMAL NODE",
                val2 = "${spec.batteryTemp}°C",
                sub2 = "${spec.batteryVoltage} mV",
                pct2 = ((spec.batteryTemp / 60f) * 100).toInt().coerceIn(0, 100),
                color2 = Color(0xFFFF9100)
            )
        }
        item {
            ExecutiveInfoCard(
                title = "Operating System & Build Details",
                items = listOf(
                    "Android Version" to "Android ${spec.androidVersion}",
                    "SDK API Level" to "API ${spec.apiLevel}",
                    "Build ID" to spec.buildId,
                    "Security Patch" to spec.securityPatch,
                    "Kernel Release" to spec.kernelVersion
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
                text = "SYSTEM SENSOR MODULES (${sensors.size})",
                color = Color.Gray,
                fontSize = 10.sp,
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vendor: ${sensor.vendor}",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "${sensor.power} mA",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- REUSABLE GLASS COMPONENTS ---

@Composable
fun ExecutiveGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassShape = RoundedCornerShape(16.dp)
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
            .background(Color(0xFF111420).copy(alpha = 0.85f))
            .border(1.dp, borderGradient, glassShape)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun ExecutiveInfoCard(title: String, items: List<Pair<String, String>>) {
    ExecutiveGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        items.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = pair.first, color = Color.Gray, fontSize = 11.sp)
                Text(
                    text = pair.second,
                    color = Color(0xFFD1D5DB),
                    fontSize = 11.sp,
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
        Text(text = title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = subText, color = Color.Gray, fontSize = 9.sp)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = (360f * (percentage / 100f)),
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(text = "$percentage%", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "LIVE",
            color = Color(0xFF00E5FF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
