package com.neon.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var vpnThread: Thread? = null

    companion object {
        const val ACTION_START_VPN = "com.neon.gamebooster.START_VPN"
        const val ACTION_STOP_VPN = "com.neon.gamebooster.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN || intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        try {
            startForegroundServiceNotification()
            setupVpn()
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            .setContentText("Game Ping Isolation Active")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    private fun setupVpn() {
        try {
            if (vpnInterface != null) return

            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
            val pm = packageManager

            val builder = Builder()
                // Valid /24 subnet address taaki Android OS interface ko reject na kare
                .addAddress("10.120.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .setSession("NeonPingIsolation")
                .setMtu(1500)
                .setBlocking(false)

            // Selected games ko safe bypass (direct network connection for zero ping)
            for (gamePkg in selectedGames) {
                try {
                    pm.getPackageInfo(gamePkg, 0)
                    builder.addDisallowedApplication(gamePkg)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Neon Game Booster app ko bhi bypass rakhein taaki Shizuku/IPC disconnect na ho
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            vpnInterface = builder.establish()

            // Fallback: Agar kisi custom ROM par default route fail ho
            if (vpnInterface == null) {
                val fallbackBuilder = Builder()
                    .addAddress("10.120.0.2", 24)
                    .addRoute("10.120.0.0", 24)
                    .setSession("NeonPingIsolation")
                vpnInterface = fallbackBuilder.establish()
            }

            if (vpnInterface != null) {
                isRunning.set(true)
                startVpnPacketLoop(vpnInterface!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVpnPacketLoop(pfd: ParcelFileDescriptor) {
        vpnThread = Thread({
            try {
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                    val length = inputStream.read(buffer)
                    if (length <= 0) {
                        Thread.sleep(100)
                    }
                }
            } catch (e: InterruptedException) {
                // Thread naturally stopped
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, "VpnPacketThread")
        vpnThread?.start()
    }

    private fun stopVpn() {
        try {
            isRunning.set(false)
            vpnThread?.interrupt()
            vpnThread = null
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
