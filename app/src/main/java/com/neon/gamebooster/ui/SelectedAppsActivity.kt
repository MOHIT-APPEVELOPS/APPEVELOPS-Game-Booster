package com.neon.gamebooster.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R
import com.neon.gamebooster.service.CrosshairService
import com.neon.gamebooster.service.GameVpnService
import com.neon.gamebooster.utils.CpuBoosterManager
import java.util.Locale

data class SelectedAppModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

class SelectedAppsActivity : AppCompatActivity() {

    // VPN Permission Launcher to handle system dialog seamlessly
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                action = GameVpnService.ACTION_START_VPN
            }
            startService(vpnIntent)
            Toast.makeText(this, "VPN Connected Successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_apps)

        // Top-left Back Arrow Button click listener
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        val rvSelectedApps = findViewById<RecyclerView>(R.id.recyclerViewApps)
        rvSelectedApps?.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        val appList = ArrayList<SelectedAppModel>()

        for (pkgName in savedSet) {
            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                appList.add(SelectedAppModel(appName, pkgName, icon))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // A to Z Alphabetical Sorting
        appList.sortBy { it.appName.lowercase(Locale.ROOT) }

        val adapter = SelectedAppsAdapter(appList) { pkgName ->
            startBoosterServicesAndLaunch(pkgName, prefs)
        }
        rvSelectedApps?.adapter = adapter
    }

    private fun startBoosterServicesAndLaunch(packageName: String, prefs: android.content.SharedPreferences) {
        try {
            // 1. Start VPN with Permission Check
            if (prefs.getBoolean("enable_vpn", false)) {
                val vpnPrepareIntent = VpnService.prepare(this)
                if (vpnPrepareIntent != null) {
                    // Agar permission nahi mili hai toh system popup launch karega
                    vpnPermissionLauncher.launch(vpnPrepareIntent)
                } else {
                    // Permission pehle se granted hai, direct start karo
                    val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                        action = GameVpnService.ACTION_START_VPN
                    }
                    startService(vpnIntent)
                }
            }

            // 2. Start Crosshair if enabled
            if (prefs.getBoolean("enable_crosshair", false)) {
                val crosshairIntent = Intent(this, CrosshairService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(crosshairIntent)
                } else {
                    startService(crosshairIntent)
                }
            }

            // 3. Apply Shizuku Optimizations
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.applyPerformanceMode()
                CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", true))
                CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", true))
                CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", true))
            }

            // 4. Launch the Target Game/App
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Unable to launch this app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class SelectedAppsAdapter(
    private val appList: List<SelectedAppModel>,
    private val onLaunchClick: (String) -> Unit
) : RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val btnLaunch: Button = itemView.findViewById(R.id.btnLaunchApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appList[position]
        holder.tvName.text = item.appName
        holder.ivIcon.setImageDrawable(item.icon)

        holder.btnLaunch.setOnClickListener {
            onLaunchClick(item.packageName)
        }
    }

    override fun getItemCount(): Int = appList.size
}
