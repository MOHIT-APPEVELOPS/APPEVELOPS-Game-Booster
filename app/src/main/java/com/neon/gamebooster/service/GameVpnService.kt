package com.neon.gamebooster.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                builder.addAddress("10.0.0.2", 24)
                builder.addRoute("0.0.0.0", 0)
                builder.addDnsServer("1.1.1.1") // Cloudflare DNS for lowest ping
                builder.addDnsServer("8.8.8.8")
                builder.setSession("NeonGameBoosterVPN")
                
                vpnInterface = builder.establish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
