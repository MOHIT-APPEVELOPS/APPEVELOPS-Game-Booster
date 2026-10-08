package com.neon.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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

        try {
            // Using CONNECTED_DEVICE type which is fully supported by your project setup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(101, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            startForeground(101, notification)
        }
    }

    private fun setupVpn() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1") // Cloudflare DNS
                    .addDnsServer("8.8.8.8") // Google DNS
                    .setSession("NeonGameBoosterVPN")
                    .setMtu(1500)
                
                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    isRunning.set(true)
                    startVpnPacketLoop(vpnInterface!!)
                }
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
                while (isRunning.get()) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        // Packets processed to keep TUN interface active
                    }
                }
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
