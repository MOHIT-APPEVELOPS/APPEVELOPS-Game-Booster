package com.neon.gamebooster.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R

class AppListActivity : AppCompatActivity() {

    private val selectedPackages = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        val listView = findViewById<ListView>(R.id.listViewApps)
        val btnSave = findViewById<Button>(R.id.btnSaveApps)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
        selectedPackages.addAll(savedSet)

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // Sirf user apps
            .sortedBy { pm.getApplicationLabel(it).toString() }

        val appNames = apps.map { pm.getApplicationLabel(it).toString() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, appNames)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        for (i in apps.indices) {
            if (selectedPackages.contains(apps[i].packageName)) {
                listView.setItemChecked(i, true)
            }
        }

        btnSave.setOnClickListener {
            selectedPackages.clear()
            val checkedPositions = listView.checkedItemPositions
            for (i in 0 until adapter.count) {
                if (checkedPositions.get(i)) {
                    selectedPackages.add(apps[i].packageName)
                }
            }

            prefs.edit().putStringSet("selected_apps", selectedPackages).apply()
            Toast.makeText(this, "Apps Saved for VPN Tunneling!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
