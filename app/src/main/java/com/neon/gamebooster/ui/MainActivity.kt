package com.neon.gamebooster.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R
import com.neon.gamebooster.service.CrosshairService
import com.neon.gamebooster.service.GameVpnService
import com.neon.gamebooster.utils.CpuBoosterManager

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Safe-call (?.) use kiya hai taaki agar button na mile toh app crash na ho
        val btnToggleBoost = findViewById<Button?>(R.id.btnToggleBoost)
        val btnCrosshairSettings = findViewById<Button?>(R.id.btnCrosshairSettings)

        btnToggleBoost?.setOnClickListener {
            try {
                CpuBoosterManager.applyPerformanceMode()
            } catch (e: Exception) {
                e.printStackTrace()
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
            
            Toast.makeText(this, "Optimization Triggered!", Toast.LENGTH_SHORT).show()
        }

        btnCrosshairSettings?.setOnClickListener {
            try {
                startService(Intent(this, CrosshairService::class.java))
                Toast.makeText(this, "Crosshair Overlay Enabled", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error starting service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVpnService() {
        try {
            startService(Intent(this, GameVpnService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        }
    }
}
