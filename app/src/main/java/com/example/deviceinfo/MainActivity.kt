package com.example.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)

        // App Header Title
        val headerTitle = TextView(this).apply {
            text = "Device Info Pro"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
        }
        val headerSub = TextView(this).apply {
            text = "Complete Hardware & System Specifications"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dpToPx(2), 0, dpToPx(20))
        }

        rootLayout.addView(headerTitle)
        rootLayout.addView(headerSub)

        // Build Spec Cards
        val categories = getDeviceSpecs(this)

        for (category in categories) {
            val cardView = createCategoryCard(category)
            rootLayout.addView(cardView)
        }
    }

    private fun createCategoryCard(category: InfoCategory): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

            val drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dpToPx(16).toFloat()
            }
            background = drawable

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
            this.layoutParams = layoutParams
        }

        val titleView = TextView(this).apply {
            text = category.title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#818CF8"))
            setPadding(0, 0, 0, dpToPx(12))
        }
        card.addView(titleView)

        category.items.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                setPadding(0, dpToPx(6), 0, dpToPx(6))
            }

            val labelView = TextView(this).apply {
                text = item.label
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
            }

            val valueView = TextView(this).apply {
                text = item.value
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#F8FAFC"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }

            row.addView(labelView)
            row.addView(valueView)
            card.addView(row)

            if (index < category.items.size - 1) {
                val divider = LinearLayout(this).apply {
                    setBackgroundColor(Color.parseColor("#334155"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                    ).apply {
                        setMargins(0, dpToPx(4), 0, dpToPx(4))
                    }
                }
                card.addView(divider)
            }
        }

        return card
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

data class InfoItem(val label: String, val value: String)
data class InfoCategory(val title: String, val items: List<InfoItem>)

fun getDeviceSpecs(context: Context): List<InfoCategory> {
    val identityItems = listOf(
        InfoItem("Brand", Build.BRAND.replaceFirstChar { it.uppercase() }),
        InfoItem("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }),
        InfoItem("Model", Build.MODEL),
        InfoItem("Device / Board", "${Build.DEVICE} (${Build.BOARD})"),
        InfoItem("Hardware", Build.HARDWARE),
        InfoItem("IMEI Status", getImeiStatus(context))
    )

    val osItems = listOf(
        InfoItem("Android Version", Build.VERSION.RELEASE),
        InfoItem("API Level", Build.VERSION.SDK_INT.toString()),
        InfoItem("Security Patch", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
        InfoItem("Build ID", Build.DISPLAY)
    )

    val cpuItems = listOf(
        InfoItem("CPU Architecture", System.getProperty("os.arch") ?: "Unknown"),
        InfoItem("CPU Cores", Runtime.getRuntime().availableProcessors().toString()),
        InfoItem("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
    )

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

    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    wm.defaultDisplay.getMetrics(metrics)

    val displayItems = listOf(
        InfoItem("Resolution", "${metrics.widthPixels} x ${metrics.heightPixels} px"),
        InfoItem("Density (DPI)", "${metrics.densityDpi} dpi")
    )

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
        "Restricted (Android 10+ Policy)"
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
