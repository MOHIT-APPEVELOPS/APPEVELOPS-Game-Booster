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
import android.view.MotionEvent
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
            .setContentText("Crosshair active - Position locked")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(102, notification)
    }

    private fun showCrosshair() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Saved position load karne ke liye
            val prefs = getSharedPreferences("CrosshairPrefs", Context.MODE_PRIVATE)
            val savedX = prefs.getInt("pos_x", 0)
            val savedY = prefs.getInt("pos_y", 1)

            val textView = TextView(this).apply {
                text = "o"
                
