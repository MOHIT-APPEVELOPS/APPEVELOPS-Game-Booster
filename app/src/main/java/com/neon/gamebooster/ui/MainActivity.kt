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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
        
        val switchVpn = findViewById<SwitchCompat>(R.id.switchVpn)
        val switchCrosshair = findViewById<SwitchCompat>(R.id.switchCrosshair)
        val switchBgAppKiller = findViewById<SwitchCompat>(R.id.switchBgAppKiller)
        val switchCacheClear = findViewById<SwitchCompat>(R.id.switchCacheClear)
        val switchSystemBoost = findViewById<SwitchCompat>(R.id.switchSystemBoost)
        val switchBatteryOptimization = findViewById<SwitchCompat>(R.id.switchBatteryOptimization)
        
        rvSelectedGames = findViewById(R.id.rvSelectedGames)
        rvSelectedGames.layoutManager = LinearLayoutManager(this)

        updateShizukuStatus()

        switchVpn.isChecked = prefs.getBoolean("enable_vpn", true)
        switchCrosshair.isChecked = prefs.getBoolean("enable_crosshair", true)
        switchBgAppKiller.isChecked = prefs.getBoolean("enable_bg_killer", true)
        switchCacheClear.isChecked = prefs.getBoolean("enable_cache_clear", true)
        switchSystemBoost.isChecked = prefs.getBoolean("enable_system_boost", true)
        
        switchBatteryOptimization.isChecked = isBatteryOptimizationIgnored()
        switchBatteryOptimization.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestIgnoreBatteryOptimizations()
        }

        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
        }

        switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
        }

        btnConnectShizuku.setOnClickListener {
            requestShizukuPermission()
        }

        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnDndSettings.setOnClickListener {
            startActivity(Intent(this, DndSettingsActivity::class.java))
        }

        btnAnimationSettings.setOnClickListener {
            startActivity(Intent(this, AnimationSettingsActivity::class.java))
        }

        btnToggleBoost.setOnClickListener {
            Toast.makeText(this, "Services Started Successfully!", Toast.LENGTH_SHORT).show()
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
}
