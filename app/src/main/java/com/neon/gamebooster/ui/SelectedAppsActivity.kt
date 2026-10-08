package com.neon.gamebooster.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_apps)

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

        val adapter = SelectedAppsAdapter(this, appList, prefs) { pkgName ->
            startBoosterServicesAndLaunch(pkgName, prefs)
        }
        rvSelectedApps?.adapter = adapter
    }

    private fun startBoosterServicesAndLaunch(packageName: String, prefs: android.content.SharedPreferences) {
        try {
            // 1. Start VPN if enabled
            if (prefs.getBoolean("enable_vpn", true)) {
                val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                    action = GameVpnService.ACTION_START_VPN
                }
                startService(vpnIntent)
            }

            // 2. Start Crosshair if enabled
            if (prefs.getBoolean("enable_crosshair", true)) {
                startService(Intent(this, CrosshairService::class.java))
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class SelectedAppsAdapter(
    private val context: Context,
    private val appList: List<SelectedAppModel>,
    private val prefs: android.content.SharedPreferences,
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
