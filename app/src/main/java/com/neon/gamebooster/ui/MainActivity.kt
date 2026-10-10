package com.neon.gamebooster.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R
import com.neon.gamebooster.utils.CpuBoosterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class HomeGameModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable?
)

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 100
    private val OVERLAY_REQUEST_CODE = 101
    private val SHIZUKU_PERMISSION_REQUEST_CODE = 102
    private val NOTIF_PERMISSION_REQUEST_CODE = 103

    private lateinit var rvSelectedGames: RecyclerView
    private lateinit var tvStatus: TextView
    private var viewStatusLight: View? = null
    private var btnConnectShizuku: Button? = null
    
    private val selectedGamesList = ArrayList<HomeGameModel>()
    private lateinit var gamesAdapter: HomeGamesAdapter

    // Shizuku Listeners to sync status in real-time
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { updateShizukuStatus() }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { updateShizukuStatus() }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Shizuku Permission Denied", Toast.LENGTH_SHORT).show()
                }
                updateShizukuStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register Shizuku Listeners
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        viewStatusLight = findViewById(R.id.viewStatusLight)
        btnConnectShizuku = findViewById(R.id.btnConnectShizuku)
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)
        val btnDndSettings = findViewById<Button>(R.id.btnDndSettings)
        val btnAnimationSettings = findViewById<Button>(R.id.btnAnimationSettings)
        val btnToggleBoost = findViewById<Button>(R.id.btnToggleBoost)
        
        val switchVpn = findViewById<Switch>(R.id.switchVpn)
        val switchCrosshair = findViewById<Switch>(R.id.switchCrosshair)
        val switchBgAppKiller = findViewById<Switch>(R.id.switchBgAppKiller)
        val switchCacheClear = findViewById<Switch>(R.id.switchCacheClear)
        val switchSystemBoost = findViewById<Switch>(R.id.switchSystemBoost)
        val switchBatteryOptimization = findViewById<Switch>(R.id.switchBatteryOptimization)
        
        // RecyclerView aur Adapter initialization ("No adapter attached" fix)
        rvSelectedGames = findViewById(R.id.rvSelectedGames)
        rvSelectedGames.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        gamesAdapter = HomeGamesAdapter(selectedGamesList) { pkgName ->
            // Click to launch game directly
            val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
        rvSelectedGames.adapter = gamesAdapter

        updateShizukuStatus()
        checkFirstLaunchPermissions()

        switchVpn?.isChecked = prefs.getBoolean("enable_vpn", true)
        switchCrosshair?.isChecked = prefs.getBoolean("enable_crosshair", true)
        switchBgAppKiller?.isChecked = prefs.getBoolean("enable_bg_killer", true)
        switchCacheClear?.isChecked = prefs.getBoolean("enable_cache_clear", true)
        switchSystemBoost?.isChecked = prefs.getBoolean("enable_system_boost", true)
        
        switchBatteryOptimization?.isChecked = isBatteryOptimizationIgnored()
        switchBatteryOptimization?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestIgnoreBatteryOptimizations()
        }

        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_vpn", isChecked).apply()
            if (isChecked) checkAndRequestVpnPermission()
        }

        switchCrosshair?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_crosshair", isChecked).apply()
            if (isChecked) checkAndRequestOverlayPermission()
        }

        btnConnectShizuku?.setOnClickListener {
            handleConnectShizukuClick()
        }

        btnSelectApps?.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        btnDndSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, DndSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "DndSettingsActivity error", Toast.LENGTH_SHORT).show()
            }
        }

        btnAnimationSettings?.setOnClickListener {
            try {
                startActivity(Intent(this, AnimationSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "AnimationSettingsActivity error", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggleBoost?.setOnClickListener {
            val intent = Intent(this, SelectedAppsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSelectedGames() {
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val savedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
            val tempList = ArrayList<HomeGameModel>()
            val pm = packageManager

            for (pkg in savedSet) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    tempList.add(HomeGameModel(label, pkg, icon))
                } catch (ignored: Exception) {}
            }

            withContext(Dispatchers.Main) {
                selectedGamesList.clear()
                selectedGamesList.addAll(tempList)
                gamesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleConnectShizukuClick() {
        val shizukuPkg = "moe.shizuku.privileged.api"

        if (!isPackageInstalled(shizukuPkg, packageManager)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$shizukuPkg"))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$shizukuPkg"))
                startActivity(intent)
            }
        } else {
            val launchIntent = packageManager.getLaunchIntentForPackage(shizukuPkg)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }

            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                try {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun checkFirstLaunchPermissions() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Neon Game Booster needs Overlay, VPN, Battery Optimization, and Accessibility permissions to optimize your gameplay effectively.")
                .setPositiveButton("Accept") { _, _ ->
                    prefs.edit().putBoolean("is_first_launch", false).apply()
                    requestInitialPermissions()
                }
                .setNegativeButton("Decline") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQUEST_CODE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            checkAndRequestOverlayPermission()
        }

        if (!isBatteryOptimizationIgnored()) {
            requestIgnoreBatteryOptimizations()
        }

        checkAndRequestVpnPermission()

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Enable Neon Game Booster Accessibility Service", Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun checkAndRequestVpnPermission() {
        try {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VPN_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "VPN Permission Granted", Toast.LENGTH_SHORT).show()
                }
            }
            OVERLAY_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay Permission Granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
        loadSelectedGames()
    }

    private fun updateShizukuStatus() {
        runOnUiThread {
            try {
                if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    tvStatus.text = "Shizuku Engine: Connected"
                    tvStatus.setTextColor(Color.parseColor("#00E676"))
                    viewStatusLight?.setBackgroundResource(R.drawable.status_light_green)
                    btnConnectShizuku?.visibility = View.GONE
                } else {
                    tvStatus.text = "Shizuku Engine: Disconnected"
                    tvStatus.setTextColor(Color.parseColor("#FF2A55"))
                    viewStatusLight?.setBackgroundResource(R.drawable.status_light_red)
                    btnConnectShizuku?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvStatus.text = "Shizuku Engine: Disconnected"
                tvStatus.setTextColor(Color.parseColor("#FF2A55"))
                viewStatusLight?.setBackgroundResource(R.drawable.status_light_red)
                btnConnectShizuku?.visibility = View.VISIBLE
            }
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Inner Adapter for Home Selected Games Horizontal List
    private class HomeGamesAdapter(
        private val list: List<HomeGameModel>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<HomeGamesAdapter.GameViewHolder>() {

        class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: ImageView? = itemView.findViewById(R.id.ivAppIcon)
            val tvName: TextView? = itemView.findViewById(R.id.tvAppName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return GameViewHolder(view)
        }

        override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
            val item = list[position]
            holder.tvName?.text = item.appName
            if (item.icon != null) {
                holder.ivIcon?.setImageDrawable(item.icon)
            }
            holder.itemView.setOnClickListener {
                onItemClick(item.packageName)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
