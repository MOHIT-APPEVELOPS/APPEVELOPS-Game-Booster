package com.neon.gamebooster.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.neon.gamebooster.R

class CrosshairService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var crosshairView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        crosshairView = LayoutInflater.from(this).inflate(R.layout.overlay_crosshair, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0 // X-Axis Offset customization
            y = 0 // Y-Axis Offset for headshot tuning (e.g. BGMI)
        }

        windowManager.addView(crosshairView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::crosshairView.isInitialized) {
            windowManager.removeView(crosshairView)
        }
    }
}
