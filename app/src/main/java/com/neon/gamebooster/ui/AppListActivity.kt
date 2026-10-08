package com.neon.gamebooster.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R

class AppListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        val rvApps = findViewById<RecyclerView>(R.id.rvApps)
        val btnSave = findViewById<Button>(R.id.btnSaveSelection)

        rvApps.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        
        // Query all launcher applications so user installed apps/games show up properly
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = ArrayList<AppModel>()

        for (ri in resolveInfos) {
            val pkgName = ri.activityInfo.packageName
            val appName = ri.loadLabel(pm).toString()
            val icon = ri.loadIcon(pm)
            
            // Exclude current app package if needed
            if (pkgName != packageName) {
                appList.add(AppModel(appName, pkgName, icon, false))
            }
        }

        val adapter = AppListAdapter(appList)
        rvApps.adapter = adapter

        btnSave?.setOnClickListener {
            finish()
        }
    }
}
