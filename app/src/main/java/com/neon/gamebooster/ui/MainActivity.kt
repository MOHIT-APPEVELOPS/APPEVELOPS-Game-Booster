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
    private lateinit var tvDevicePerfInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        tvDevicePerfInfo = findViewById(R.id.tvDevicePerfInfo)
        
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)
        val btnToggleBoost = findViewById<Button>(R.id.btnToggleBoost)
        
        val switchVpn = findViewById<SwitchCompat>(R.id.switchVpn)
        val switchCrosshair = findViewById<SwitchCompat>(R.id.switchCrosshair)
        val switchWindowAnim = findViewById<SwitchCompat>(R.id.switchWindowAnim)
        val switchTransitionAnim = findViewById<SwitchCompat>(R.id.switchTransitionAnim)
        val switchAnimatorAnim = findViewById<SwitchCompat>(R.id.switchAnimatorAnim)
        val switchBatteryOptimization = findViewById<SwitchCompat>(R.id.switchBatteryOptimization)
        
        rvSelectedGames = findViewById(R.id.rvSelectedGames)
        rvSelectedGames.layoutManager = LinearLayoutManager(this)

        updateShizukuStatus()
        updateDevicePerformanceDisplay()

        switchVpn.isChecked = prefs.getBoolean("enable_vpn", true)
        switchCrosshair.isChecked = prefs.getBoolean("enable_crosshair", true)
        switchWindowAnim.isChecked = prefs.getBoolean("enable_window_anim", true)
        switchTransitionAnim.isChecked = prefs.getBoolean("enable_transition_anim", true)
        switchAnimatorAnim.isChecked = prefs.getBoolean("enable_animator_anim", true)
        
        if (switchBatteryOptimization != null) {
            switchBatteryOptimization.isChecked = isBatteryOptimizationIgnored()
            switchBatteryOptimization.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    requestIgnoreBatteryOptimizations()
                }
            }
        }

        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
        }

        switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            }
        }

        switchWindowAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_window_anim", isChecked).apply()
        }

        switchTransitionAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_transition_anim", isChecked).apply()
        }

        switchAnimatorAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_animator_anim", isChecked).apply()
        }

        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnToggleBoost.setOnClickListener {
            if (!CpuBoosterManager.isShizukuAvailableAndGranted()) {
                requestShizukuPermission()
            } else {
                try {
                    CpuBoosterManager.applyPerformanceMode()
                    CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", true))
                    CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", true))
                    CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", true))
                    Toast.makeText(this, "Booster & Custom Animation Scales Active!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
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
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun updateDevicePerformanceDisplay() {
        try {
            val info = CpuBoosterManager.getDevicePerformanceInfo(this)
            tvDevicePerfInfo?.text = "Device Specs: $info"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVpnService() {
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

    private fun startCrosshairService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        } else {
            val intent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
        updateDevicePerformanceDisplay()
        loadSelectedGames()
    }

    private fun updateShizukuStatus() {
        if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
            tvStatus.text = "Shizuku Engine: Active & Ready (Non-Root Secure)"
        } else {
            tvStatus.text = "Shizuku Engine: Not Connected / Click to Download"
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                }
            } else {
                Toast.makeText(this, "Shizuku Service is not running on device!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Shizuku Integration Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSelectedGames() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val selectedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        val pm = packageManager
        val list = ArrayList<GameModel>()

        for (pkg in selectedSet) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                list.add(GameModel(name, pkg, icon))
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        val adapter = SelectedGamesAdapter(list) { game ->
            if (!CpuBoosterManager.isShizukuAvailableAndGranted() && prefs.getBoolean("enable_window_anim", true)) {
                Toast.makeText(this, "Shizuku required for Custom Animation Speed Optimization!", Toast.LENGTH_LONG).show()
                requestShizukuPermission()
            } else {
                Toast.makeText(this, "Applying Settings & Launching Game...", Toast.LENGTH_SHORT).show()

                if (prefs.getBoolean("enable_vpn", true)) {
                    startVpnService()
                }
                if (prefs.getBoolean("enable_crosshair", true)) {
                    startCrosshairService()
                }

                try {
                    CpuBoosterManager.autoCleanAndBoostSystem(this, game.packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", true))
                    CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", true))
                    CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", true))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    CpuBoosterManager.applyPerformanceMode()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val launchIntent = pm.getLaunchIntentForPackage(game.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "Unable to launch game", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvSelectedGames.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            val intent = Intent(this, GameVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "VPN Ping Isolation Active", Toast.LENGTH_SHORT).show()
        } else if (requestCode == OVERLAY_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Crosshair Overlay Active", Toast.LENGTH_SHORT).show()
        }
    }
}
Animation Speed Optimization!", Toast.LENGTH_LONG).show()
                requestShizukuPermission()
            } else {
                Toast.makeText(this, "Applying Settings & Launching Game...", Toast.LENGTH_SHORT).show()

                // 1. Start active VPN & Crosshair services if switches are ON specifically for this game session
                if (prefs.getBoolean("enable_vpn", true)) {
                    startVpnService()
                }
                if (prefs.getBoolean("enable_crosshair", true)) {
                    startCrosshairService()
                }

                // 2. Perform Background App Kill & Cleanup
                try {
                    CpuBoosterManager.autoCleanAndBoostSystem(this, game.packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. Apply Animation Scales individually based on user toggle preferences
                try {
                    CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", true))
                    CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", true))
                    CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", true))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. Apply CPU Performance Mode
                try {
                    CpuBoosterManager.applyPerformanceMode()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 5. Launch Game
                val launchIntent = pm.getLaunchIntentForPackage(game.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "Unable to launch game", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvSelectedGames.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            val intent = Intent(this, GameVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "VPN Ping Isolation Active", Toast.LENGTH_SHORT).show()
        } else if (requestCode == OVERLAY_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Crosshair Overlay Active", Toast.LENGTH_SHORT).show()
        }
    }
}
 {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "Unable to launch game", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvSelectedGames.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE) {
            val intent = Intent(this, GameVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "VPN Ping Isolation Active", Toast.LENGTH_SHORT).show()
        } else if (requestCode == OVERLAY_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Crosshair Overlay Active", Toast.LENGTH_SHORT).show()
        }
    }
}
