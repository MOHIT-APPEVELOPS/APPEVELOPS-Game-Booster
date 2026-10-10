package com.neon.gamebooster.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class AppModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    var isSelected: Boolean = false
)

class AppListActivity : AppCompatActivity() {

    private lateinit var adapter: AppListAdapter
    private val appList = ArrayList<AppModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        // Top-left Back Arrow Button click listener
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        val rvApps = findViewById<RecyclerView>(R.id.rvApps)
        val btnSave = findViewById<Button>(R.id.btnSaveSelection)

        // Adapter pehle hi attach karein taaki "No adapter attached" warning na aaye
        adapter = AppListAdapter(appList)
        rvApps?.layoutManager = LinearLayoutManager(this)
        rvApps?.adapter = adapter

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        // Background Thread (Dispatchers.IO) me Apps load karein (No UI Lag/Freeze)
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val savedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
            val tempList = ArrayList<AppModel>()

            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                if (pkgName != packageName) {
                    val appName = ri.loadLabel(pm).toString()
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }
                    val isSelected = savedSet.contains(pkgName)
                    tempList.add(AppModel(appName, pkgName, icon, isSelected))
                }
            }

            // A to Z Alphabetical Sorting
            tempList.sortBy { it.appName.lowercase(Locale.ROOT) }

            // UI Thread par data update karein
            withContext(Dispatchers.Main) {
                appList.clear()
                appList.addAll(tempList)
                adapter.notifyDataSetChanged()
            }
        }

        btnSave?.setOnClickListener {
            val selectedPackages = appList.filter { it.isSelected }.map { it.packageName }.toSet()
            prefs.edit().putStringSet("selected_apps", selectedPackages).apply()
            finish()
        }
    }
}

class AppListAdapter(private val appList: List<AppModel>) :
    RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView? = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = appList[position]
        holder.tvName.text = item.appName

        if (item.icon != null) {
            holder.ivIcon?.setImageDrawable(item.icon)
        }

        // Checkbox recycling bug fix
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = item.isSelected

        holder.itemView.setOnClickListener {
            item.isSelected = !item.isSelected
            holder.cbSelect.isChecked = item.isSelected
        }

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }
    }

    override fun getItemCount(): Int = appList.size
    }
    
