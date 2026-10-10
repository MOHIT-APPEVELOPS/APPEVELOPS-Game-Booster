package com.neon.gamebooster.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neon.gamebooster.R

class PermissionsActivity : AppCompatActivity() {

    private val REQ_CODE_NOTIF = 201

    private var switchOverlay: Switch? = null
    private var switchAccessibility: Switch? = null
    private var switchBattery: Switch? = null
    private var switchNotification: Switch? = null
    private var switchDnd: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        switchOverlay = findViewById(R.id.switchPermOverlay)
        switchAccessibility = findViewById(R.id.switchPermAccessibility)
        switchBattery = findViewById(R.id.switchPermBattery)
        switchNotification = findViewById(R.id.switchPermNotification)
        switchDnd = findViewById(R.id.switchPermDnd)

        // 1. Overlay Switch Listener
        switchOverlay?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        startActivity(Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ))
                    }
                } else {
                    Toast.makeText(this, "Disable overlay from system settings", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                }
            }
        }

        // 2. Accessibility Switch Listener
        switchAccessibility?.setOnCheckedChangeListener { buttonView, _ ->
            if (buttonView.isPressed) {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Battery Optimization Switch Listener
        switchBattery?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationIgnored()) {
                        try {
                            startActivity(Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:$packageName")
                            ))
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                } else {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }

        // 4. Notification Switch Listener
        switchNotification?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_CODE_NOTIF)
                        }
                    } else {
                        openAppNotificationSettings()
                    }
                } else {
                    openAppNotificationSettings()
                }
            }
        }

        // 5. DND Policy Switch Listener
        switchDnd?.setOnCheckedChangeListener { buttonView, _ ->
            if (buttonView.isPressed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        // Overlay Status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switchOverlay?.isChecked = Settings.canDrawOverlays(this)
        } else {
            switchOverlay?.isChecked = true
        }

        // Accessibility Status
        switchAccessibility?.isChecked = isAccessibilityServiceEnabled()

        // Battery Optimization Status
        switchBattery?.isChecked = isBatteryOptimizationIgnored()

        // Notification Status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switchNotification?.isChecked = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            switchNotification?.isChecked = nm.areNotificationsEnabled()
        }

        // DND Policy Access Status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            switchDnd?.isChecked = nm.isNotificationPolicyAccessGranted
        } else {
            switchDnd?.isChecked = true
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", packageName)
                putExtra("app_uid", applicationInfo.uid)
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
