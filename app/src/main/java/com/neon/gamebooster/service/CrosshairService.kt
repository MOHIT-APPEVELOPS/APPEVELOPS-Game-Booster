package com.neon.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class CrosshairService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        showCrosshair()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "crosshair_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crosshair Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Neon Crosshair")
            .setContentText("Crosshair is running over games")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(102, notification)
    }

    private fun showCrosshair() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Safe crosshair creation without extra layout dependencies
            val textView = TextView(this).apply {
                text = "+"
                textSize = 32f
                setTextColor(Color.RED)
                gravity = Gravity.CENTER
            }
            crosshairView = textView

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLATION_MODES_DEFAULT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager?.addView(crosshairView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (crosshairView != null && windowManager != null) {
            try {
                windowManager?.removeView(crosshairView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
