<div align="center">

  <!-- Matrix Animated Header Banner -->
  <img src="https://capsule-render.vercel.app/api?type=matrix&color=00FF9D&height=200&section=header&text=DEVICE%20INSIGHT%20HUD&fontSize=42&fontColor=00FF9D&animation=twinkle&fontAlignY=42" width="100%" alt="Matrix Telemetry Header" />

  <p><strong>SYSTEM DIAGNOSTIC & HARDWARE TELEMETRY CONSOLE • v3.0.0</strong></p>
  <p><em>Architected & Developed by <strong>SANDEEP SOM</strong> • <code>@sandeep26dc</code></em></p>

  <br>

  <!-- Status LED Badges -->
  <a href="https://github.com/sandeep26dc/DeviceInfoApp/actions">
    <img src="https://img.shields.io/badge/SYS_STATUS-OPTIMAL-00FF9D?style=for-the-badge&logo=gnubash&logoColor=black" alt="Status">
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/KERNEL-KOTLIN_2.0-00FF9D?style=for-the-badge&logo=kotlin&logoColor=black" alt="Kotlin">
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/HUD-COMPOSE_UI-00F0FF?style=for-the-badge&logo=android&logoColor=black" alt="Compose">
  </a>
  <a href="https://github.com/sandeep26dc/DeviceInfoApp/releases">
    <img src="https://img.shields.io/badge/DEPLOY-DEBUG_APK-05070A?style=for-the-badge&logo=android&logoColor=00FF9D" alt="APK">
  </a>

  <br><br>

  <!-- Device Frame Mockup -->
  <img src="docs/screenshots/terminal_preview.png" width="340" alt="DeviceInfoApp HUD Interface" style="border: 2px solid #00FF9D; border-radius: 12px; box-shadow: 0 0 25px rgba(0, 255, 157, 0.4);">

  <br><br>

  <p width="82%">
    <code>DEVICE_INFO_APP</code> is a deep-level hardware diagnostic suite for Android. Architected by <strong>SANDEEP SOM</strong>, it bypasses high-level OS abstracts to stream raw hardware metrics—from real-time CPU core frequencies and thermal throttling limits to battery wear states and live RAM allocation.
  </p>

</div>

---

## 💻 `SYS_DIAGNOSTICS` // Functional Index

```text
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                   M A T R I X   D I A G N O S T I C S                   │
 ├───────────────────┬──────────────────────┬──────────────────────────────┤
 │  01 // SOC & CPU  │  02 // THERMAL MATRIX│  03 // MEMORY & STORAGE      │
 ├───────────────────┼──────────────────────┼──────────────────────────────┤
 │ • Core Frequencies│ • Battery Volts (mV) │ • Active RAM Allocation      │
 │ • Big.LITTLE Loads│ • Temp Thresholds    │ • Swap / Cache Metrics       │
 │ • ABI & Instruction│ • Thermal Throttling │ • Flash Storage Blocks       │
 └───────────────────┴──────────────────────┴──────────────────────────────┘
