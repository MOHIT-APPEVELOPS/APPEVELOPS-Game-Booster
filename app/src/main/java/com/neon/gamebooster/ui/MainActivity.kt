package com.neon.gamebooster.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R
import com.neon.gamebooster.service.CrosshairService
import com.neon.gamebooster.service.GameVpnService
import com.neon.gamebooster.utils.CpuBoosterManager
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 100
    private val OVERLAY_REQUEST_CODE = 101
    private val SHIZUKU_PERMISSION_REQUEST_CODE = 102
    private val NOTIF_PERMISSION_REQUEST_CODE = 103

    private lateinit var rvSelectedGames: RecyclerView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        val btnConnectShizuku = findViewById<Button>(R.id.btnConnectShizuku)
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)
        val btnDndSettings = findViewById<Button>(R.id.btnDndSettings)
        val btnAnimationSettings = findViewById<Button>(R.id.btnAnimationSettings)
        val btnToggleBoost = findViewById<Button>(R.id.btnToggleBoost)
        
        val switchVpn = findViewById<Switch>(R.id.switchVpn)
        val switchCrosshair = findViewById<Switch>(R.id.switchCrosshair)
        val switchBgAppKiller = findViewById<Switch>(R.id.switchBgAppKiller)
        val switchCacheClear = findViewById<Switch>(R.id.switchCacheClear)
        val switchSystemBoost = findViewById<Switch>(R.id.switchSystemBoost)
        val switchBatteryOptimization = findViewById<Switch>(R.id.switchBatteryOptimization)
        
        rvSelectedGames = findViewById(R.id.rvSelectedGames)
        rvSelectedGames.layoutManager = LinearLayoutManager(this)

        updateShizukuStatus()

        // App Start hote hi Permissions lene ke liye
        checkFirstLaunchPermissions()

        switchVpn?.isChecked = prefs.getBoolean("enable_vpn", false)
        switchCrosshair?.isChecked = prefs.getBoolean("enable_crosshair", false)
        switchBgAppKiller?.isChecked = prefs.getBoolean("enable_bg_killer", true)
        switchCacheClear?.isChecked = prefs.getBoolean("enable_cache_clear", true)
        switchSystemBoost?.isChecked = prefs.getBoolean("enable_system_boost", true)
        
        switchBatteryOptimization?.isChecked = isBatteryOptimizationIgnored()
        switchBatteryOptimization?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestIgnoreBatteryOptimizations()
        }

        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
            if (isChecked) checkAndRequestVpnPermission()
        }

        switchCrosshair?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
            if (isChecked) checkAndRequestOverlayPermission()
        }

        btnConnectShizuku?.setOnClickListener {
            requestShizukuPermission()
        }

        btnSelectApps?.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnDndSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, DndSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "DndSettingsActivity error", Toast.LENGTH_SHORT).show()
            }
        }

        btnAnimationSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, AnimationSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "AnimationSettingsActivity error", Toast.LENGTH_SHORT).show()
            }
        }

        // Updated: START SERVICES button ab SelectedAppsActivity wali window kholega
        btnToggleBoost?.setOnClickListener {
            val intent = Intent(this, SelectedAppsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkFirstLaunchPermissions() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Neon Game Booster needs Display Overlay, Battery Optimization, Notification, VPN, and Accessibility permissions to optimize your gameplay effectively.")
                .setPositiveButton("Accept") { _, _ ->
                    prefs.edit().putBoolean("is_first_launch", false).apply()
                    requestAllAppPermissions()
                }
                .setNegativeButton("Decline") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Some features may not work without permissions.", Toast.LENGTH_LONG).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun requestAllAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQUEST_CODE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            checkAndRequestOverlayPermission()
        }

        if (!isBatteryOptimizationIgnored()) {
            requestIgnoreBatteryOptimizations()
        }

        checkAndRequestVpnPermission()

        // Accessibility Service check & prompt
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable Accessibility Service for DND & Auto-Cleaner", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun checkAndRequestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VPN_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "VPN Connected Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            OVERLAY_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Overlay Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    private fun updateShizukuStatus() {
        try {
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                tvStatus.text = "Shizuku Engine: Connected"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                tvStatus.text = "Shizuku Engine: Disconnected"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        } catch (e: Exception) {
            tvStatus.text = "Shizuku Engine: Not Installed"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!isPackageInstalled("moe.shizuku.privileged.api", packageManager)) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                startActivity(intent)
                Toast.makeText(this, "Please install Shizuku from Play Store", Toast.LENGTH_LONG).show()
                return
            }
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                } else {
                    Toast.makeText(this, "Shizuku is already connected!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Shizuku service is not running!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Shizuku Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
