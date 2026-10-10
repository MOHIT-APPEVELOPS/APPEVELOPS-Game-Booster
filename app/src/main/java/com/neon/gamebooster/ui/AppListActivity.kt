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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R
import java.util.Locale

data class AppModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)

class AppListActivity : AppCompatActivity() {

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

        rvApps?.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = ArrayList<AppModel>()
        
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        for (ri in resolveInfos) {
            val pkgName = ri.activityInfo.packageName
            val appName = ri.loadLabel(pm).toString()
            val icon = ri.loadIcon(pm)
            
            if (pkgName != packageName) {
                val isSelected = savedSet.contains(pkgName)
                appList.add(AppModel(appName, pkgName, icon, isSelected))
            }
        }

        // A to Z Alphabetical Sorting
        appList.sortBy { it.appName.lowercase(Locale.ROOT) }

        val adapter = AppListAdapter(appList)
        rvApps?.adapter = adapter

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
        
        // RecyclerView recycling fix
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
    
