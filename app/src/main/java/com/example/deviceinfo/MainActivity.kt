package com.example.deviceinfo

import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build UI programmatically to eliminate XML view layout crash points
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF0F172A.toInt()) // Sleek dark canvas background
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
        }

        // Header Title
        val titleText = TextView(this).apply {
            text = "Device Specs & Hardware"
            textSize = 24f
            setTextColor(0xFF00E5FF.toInt())
            setPadding(0, 0, 0, 48)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(titleText)

        // Populate System Details Safely
        addInfoCard(rootLayout, "Device Model", "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}")
        addInfoCard(rootLayout, "Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        addInfoCard(rootLayout, "Board / Hardware", "${Build.BOARD} / ${Build.HARDWARE}")
        addInfoCard(rootLayout, "Device Fingerprint", Build.FINGERPRINT)
        addInfoCard(rootLayout, "CPU Architecture", getCpuAbiSafe())
        addInfoCard(rootLayout, "Root Status", if (checkRootSafe()) "Rooted" else "Not Rooted")

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    private fun addInfoCard(parent: LinearLayout, label: String, value: String) {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xFF1E293B.toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(0x99FFFFFF.toInt())
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 0)
        }

        cardLayout.addView(labelView)
        cardLayout.addView(valueView)
        parent.addView(cardLayout)
    }

    private fun getCpuAbiSafe(): String {
        return try {
            Build.SUPPORTED_ABIS.joinToString(", ")
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun checkRootSafe(): Boolean {
        return try {
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
            paths.any { File(it).exists() }
        } catch (e: Exception) {
            false
        }
    }
}
