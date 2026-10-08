package com.neon.gamebooster.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class CrosshairService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // WRAP_CONTENT se yeh sirf '+' symbol jitni jagah lega, bada box nahi banega
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

            // Exact 29sp size ka '+' crosshair TextView
            crosshairView = TextView(this).apply {
                text = "+"
                textSize = 29f
                setTextColor(Color.RED) // Aap chaho toh Color.GREEN bhi kar sakte hain
                includeFontPadding = false
                gravity = Gravity.CENTER
            }

            windowManager?.addView(crosshairView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (crosshairView != null && windowManager != null) {
                windowManager?.removeView(crosshairView)
                crosshairView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
