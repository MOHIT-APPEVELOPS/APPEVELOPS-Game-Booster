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
import com.neon.gamebooster.utils.CpuBoosterManager

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 100
    private val OVERLAY_REQUEST_CODE = 101
    private lateinit var rvSelectedGames: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)
        val btnToggleBoost = findViewById<Button>(R.id.btnToggleBoost)
        val switchVpn = findViewById<SwitchCompat>(R.id.switchVpn)
        val switchCrosshair = findViewById<SwitchCompat>(R.id.switchCrosshair)
        rvSelectedGames = findViewById(R.id.rvSelectedGames)

        rvSelectedGames.layoutManager = LinearLayoutManager(this)

        tvStatus.text = "Shizuku & Booster: Ready"

        switchVpn.isChecked = prefs.getBoolean("enable_vpn", true)
        switchCrosshair.isChecked = prefs.getBoolean("enable_crosshair", true)

        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
            if (isChecked) {
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) {
                    startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
                }
            }
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

        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnToggleBoost.setOnClickListener {
            try {
                CpuBoosterManager.applyPerformanceMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Enable 'Neon Game Booster Service' in Accessibility Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                Toast.makeText(this, "Booster Service Active!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSelectedGames()
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
            // Boost Performance before Launching
            try {
                CpuBoosterManager.applyPerformanceMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Launch Game
            val launchIntent = pm.getLaunchIntentForPackage(game.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
            }
        }
        rvSelectedGames.adapter = adapter
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return prefString?.contains(packageName) == true
    }
}
