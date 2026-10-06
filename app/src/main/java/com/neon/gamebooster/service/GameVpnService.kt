package com.neon.gamebooster.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer

class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpnTunnel()
        }
        return START_STICKY
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("NeonGameBoosterVPN")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()
            isRunning = true

            Thread {
                runLoopbackFilter()
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runLoopbackFilter() {
        val inputStream = FileInputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)
        while (isRunning) {
            try {
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    // Background app traffic isolation logic
                }
            } catch (e: Exception) {
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
    }
}
