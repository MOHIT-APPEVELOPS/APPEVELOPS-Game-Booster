package com.neon.gamebooster.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
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
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)
        val btnToggleBoost = findViewById<Button>(R.id.btnToggleBoost)
        
        val switchVpn = findViewById<SwitchCompat>(R.id.switchVpn)
        val switchCrosshair = findViewById<SwitchCompat>(R.id.switchCrosshair)
        val switchWindowAnim = findViewById<SwitchCompat>(R.id.switchWindowAnim)
        val switchTransitionAnim = findViewById<SwitchCompat>(R.id.switchTransitionAnim)
        val switchAnimatorAnim = findViewById<SwitchCompat>(R.id.switchAnimatorAnim)
        
        rvSelectedGames = findViewById(R.id.rvSelectedGames)
        rvSelectedGames.layoutManager = LinearLayoutManager(this)

        updateShizukuStatus()

        // Load saved states for switches
        switchVpn.isChecked = prefs.getBoolean("enable_vpn", true)
        switchCrosshair.isChecked = prefs.getBoolean("enable_crosshair", true)
        switchWindowAnim.isChecked = prefs.getBoolean("enable_window_anim", true)
        switchTransitionAnim.isChecked = prefs.getBoolean("enable_transition_anim", true)
        switchAnimatorAnim.isChecked = prefs.getBoolean("enable_animator_anim", true)

        // VPN Switch Listener
        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
            if (isChecked) {
                startVpnService()
            } else {
                val intent = Intent(this, GameVpnService::class.java).apply { action = "STOP" }
                startService(intent)
            }
        }

        // Crosshair Switch Listener
        switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
            if (isChecked) {
                startCrosshairService()
            } else {
                stopService(Intent(this, CrosshairService::class.java))
            }
        }

        // Window Animation Scale Switch
        switchWindowAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_window_anim", isChecked).apply()
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.setWindowAnimationScale(isChecked)
            } else if (isChecked) {
                requestShizukuPermission()
            }
        }

        // Transition Animation Scale Switch
        switchTransitionAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_transition_anim", isChecked).apply()
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.setTransitionAnimationScale(isChecked)
            } else if (isChecked) {
                requestShizukuPermission()
            }
        }

        // Animator Duration Scale Switch
        switchAnimatorAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_animator_anim", isChecked).apply()
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.setAnimatorDurationScale(isChecked)
            } else if (isChecked) {
                requestShizukuPermission()
            }
        }

        // App Selection Button
        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        // Manual Boost Button
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
        loadSelectedGames()
    }

    private fun updateShizukuStatus() {
        if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
            tvStatus.text = "Shizuku Engine: Active & Ready"
        } else {
            tvStatus.text = "Shizuku Engine: Not Connected / Permission Required"
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
            if (!CpuBoosterManager.isShizukuAvailableAndGranted()) {
                Toast.makeText(this, "Shizuku required for Custom Animation Speed Optimization!", Toast.LENGTH_LONG).show()
                requestShizukuPermission()
            } else {
                Toast.makeText(this, "Applying Settings & Launching Game...", Toast.LENGTH_SHORT).show()

                // 1. Start active VPN & Crosshair services if switches are ON
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
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
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
