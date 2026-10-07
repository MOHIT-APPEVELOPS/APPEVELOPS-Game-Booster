package com.neon.gamebooster.ui

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neon.gamebooster.R
import com.neon.gamebooster.service.CrosshairService
import com.neon.gamebooster.service.GameVpnService
import com.neon.gamebooster.utils.CpuBoosterManager

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 100
    private val OVERLAY_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tvStatus = findViewById<TextView?>(R.id.tvStatus)
        val btnSelectApps = findViewById<Button?>(R.id.btnSelectApps)
        val btnToggleBoost = findViewById<Button?>(R.id.btnToggleBoost)
        val btnCrosshairSettings = findViewById<Button?>(R.id.btnCrosshairSettings)

        tvStatus?.text = "Shizuku & VPN: Ready"

        btnSelectApps?.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnToggleBoost?.setOnClickListener {
            try {
                CpuBoosterManager.applyPerformanceMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Check Accessibility permission for auto-disconnect on exit
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Please enable Game Booster Accessibility Service for Auto-Disconnect", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }

            try {
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) {
                    startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
                } else {
                    startVpnService()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            Toast.makeText(this, "Optimization & Auto-VPN Active!", Toast.LENGTH_SHORT).show()
        }

        btnCrosshairSettings?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            } else {
                startCrosshairService()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return prefString?.contains(packageName) == true
    }

    private fun startCrosshairService() {
        try {
            val intent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Crosshair Overlay Enabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, GameVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        } else if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startCrosshairService()
            }
        }
    }
}
