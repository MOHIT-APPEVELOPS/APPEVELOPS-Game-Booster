package com.neon.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_STOP_VPN = "com.neon.gamebooster.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN || intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        setupVpn()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VPN Ping Isolation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Neon Game Booster VPN")
            .setContentText("Ping Isolation Active - Gaming DNS Enabled")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    private fun setupVpn() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                builder.addAddress("10.0.0.2", 24)
                builder.addRoute("0.0.0.0", 0)
                builder.addDnsServer("1.1.1.1") // Cloudflare Low-Latency DNS
                builder.addDnsServer("8.8.8.8") // Google DNS Backup
                builder.setSession("NeonGameBoosterVPN")
                builder.setMtu(1500)
                
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
