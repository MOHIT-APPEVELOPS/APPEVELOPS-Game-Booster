package com.neon.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class CrosshairService : Service() {

    companion object {
        const val ACTION_START = "com.neon.gamebooster.ACTION_START_CROSSHAIR"
        const val ACTION_STOP = "com.neon.gamebooster.ACTION_STOP_CROSSHAIR"
        const val ACTION_HIDE = "com.neon.gamebooster.ACTION_HIDE_CROSSHAIR"
        const val ACTION_SHOW = "com.neon.gamebooster.ACTION_SHOW_CROSSHAIR"
        private const val NOTIFICATION_ID = 102
    }

    private var windowManager: WindowManager? = null
    private var crosshairView: View? = null
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
            ).apply {
                description = "Gaming crosshair overlay"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Neon Crosshair Active")
            .setContentText("Precision Micro Crosshair Running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val sizePx = (20 * density).toInt()

            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val offsetX = prefs.getInt("crosshair_offset_x", 0)
            val offsetY = prefs.getInt("crosshair_offset_y", 0)

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                x = offsetX
                y = offsetY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            crosshairView = SlimCrosshairView(this)

            windowManager?.addView(crosshairView, params)
            isViewAttached = true
            crosshairView?.visibility = View.VISIBLE
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
            ACTION_HIDE -> {
                hideCrosshair()
            }
            ACTION_SHOW -> {
                if (isViewAttached) {
                    crosshairView?.visibility = View.VISIBLE
                } else {
                    showCrosshair()
                }
            }
            ACTION_STOP -> {
                removeCrosshairView()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
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
        removeCrosshairView()
        super.onDestroy()
    }

    private class SlimCrosshairView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0033")
            strokeWidth = 1.2f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
        }

        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00FF66")
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f

            canvas.drawCircle(cx, cy, 1.2f * density, dotPaint)

            val gap = 2.5f * density
            val length = 5.5f * density

            canvas.drawLine(cx - gap - length, cy, cx - gap, cy, linePaint)
            canvas.drawLine(cx + gap, cy, cx + gap + length, cy, linePaint)
            canvas.drawLine(cx, cy - gap - length, cx, cy - gap, linePaint)
            canvas.drawLine(cx, cy + gap, cx, cy + gap + length, linePaint)
        }
    }
}
