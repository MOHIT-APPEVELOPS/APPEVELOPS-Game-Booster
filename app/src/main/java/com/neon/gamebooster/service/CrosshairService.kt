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

    companion object {
        const val ACTION_START = "com.neon.gamebooster.ACTION_START_CROSSHAIR"
        const val ACTION_STOP = "com.neon.gamebooster.ACTION_STOP_CROSSHAIR"
        const val ACTION_HIDE = "com.neon.gamebooster.ACTION_HIDE_CROSSHAIR"
        const val ACTION_SHOW = "com.neon.gamebooster.ACTION_SHOW_CROSSHAIR"
    }

    private var windowManager: WindowManager? = null
    private var crosshairView: TextView? = null
    private var isViewAttached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        showCrosshair()
    }

    private fun startForegroundNotification() {
        val channelId = "crosshair_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crosshair Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Neon Crosshair Active")
            .setContentText("+ 29sp Crosshair Running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(102, notification)
    }

    private fun showCrosshair() {
        if (isViewAttached) {
            crosshairView?.visibility = View.VISIBLE
            return
        }

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            crosshairView = TextView(this).apply {
                text = "+"
                textSize = 29f
                setTextColor(Color.RED)
                includeFontPadding = false
                gravity = Gravity.CENTER
            }

            windowManager?.addView(crosshairView, params)
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideCrosshair() {
        try {
            crosshairView?.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeCrosshairView() {
        try {
            if (isViewAttached && crosshairView != null && windowManager != null) {
                windowManager?.removeView(crosshairView)
                isViewAttached = false
                crosshairView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> hideCrosshair()
            ACTION_SHOW -> {
                if (isViewAttached) {
                    crosshairView?.visibility = View.VISIBLE
                } else {
                    showCrosshair()
                }
            }
            ACTION_STOP -> stopSelf()
            else -> {
                if (isViewAttached) {
                    crosshairView?.visibility = View.VISIBLE
                } else {
                    showCrosshair()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeCrosshairView()
    }
}
