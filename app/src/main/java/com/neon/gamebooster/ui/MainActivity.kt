package com.neon.gamebooster.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        switchVpn?.isChecked = prefs.getBoolean("enable_vpn", false)
        switchCrosshair?.isChecked = prefs.getBoolean("enable_crosshair", false)
        switchBgAppKiller?.isChecked = prefs.getBoolean("enable_bg_killer", true)
        switchCacheClear?.isChecked = prefs.getBoolean("enable_cache_clear", true)
        switchSystemBoost?.isChecked = prefs.getBoolean("enable_system_boost", true)
        
        switchBatteryOptimization?.isChecked = isBatteryOptimizationIgnored()
        switchBatteryOptimization?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestIgnoreBatteryOptimizations()
        }

        // VPN Toggle & Permission Request
        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
            if (isChecked) {
                checkAndRequestVpnPermission()
            }
        }

        // Floating Crosshair Permission Request
        switchCrosshair?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
            if (isChecked) {
                checkAndRequestOverlayPermission()
            }
        }

        btnConnectShizuku?.setOnClickListener {
            requestShizukuPermission()
        }

        btnSelectApps?.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        // DND Settings Page Navigation
        btnDndSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, DndSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "DndSettingsActivity is not registered in Manifest!", Toast.LENGTH_SHORT).show()
            }
        }

        // Animation Settings Page Navigation
        btnAnimationSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, AnimationSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "AnimationSettingsActivity is not registered in Manifest!", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggleBoost?.setOnClickListener {
            startAllActiveServices()
        }
    }

    private fun checkAndRequestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        } else {
            Toast.makeText(this, "VPN Permission Already Granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' permission for Crosshair", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Display Overlay Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAllActiveServices() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean("enable_vpn", false)) {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
            } else {
                val intent = Intent(this, GameVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }

        if (prefs.getBoolean("enable_crosshair", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                checkAndRequestOverlayPermission()
            } else {
                val intent = Intent(this, CrosshairService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }

        Toast.makeText(this, "Booster Services Started Successfully!", Toast.LENGTH_SHORT).show()
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
        if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
            tvStatus.text = "Shizuku Engine: Connected"
        } else {
            tvStatus.text = "Shizuku Engine: Not Connected"
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                }
            } else {
                Toast.makeText(this, "Shizuku is not running!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Shizuku Error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "VPN Permission Granted!", Toast.LENGTH_SHORT).show()
        } else if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Display Overlay Permission Granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
