package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import java.util.ArrayList
import kotlinx.android.synthetic.main.shortcut_activity.*

@SuppressLint("Registered")
class ShortcutActivity:Activity(), AdapterView.OnItemSelectedListener {

    private val appIntentList by lazy { getAvailableAppList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getSharedPreferences("utility", MODE_PRIVATE)

        val pkgF7 = pref.getString("shortcut_f7", null)
        val pkgF8 = pref.getString("shortcut_f8", null)
        val pkgF9 = pref.getString("shortcut_f9", null)
        val pkfF10 = pref.getString("shortcut_f10", null)

        val appTitles = ArrayList<String>()

        appTitles.add("No shortcut")
        for (intent in appIntentList) {
            appTitles.add(intent.`package`)
        }

        val adapter = ApplicationAdapter(this, R.layout.applist_dropdown_item_1line, appTitles, appList!!)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)

        shortcut_f7.adapter = adapter
        shortcut_f8.adapter = adapter
        shortcut_f9.adapter = adapter
        shortcut_f10.adapter = adapter

        shortcut_f7.setSelection(appTitles.indexOf(pkgF7))
        shortcut_f8.setSelection(appTitles.indexOf(pkgF8))
        shortcut_f9.setSelection(appTitles.indexOf(pkgF9))
        shortcut_f10.setSelection(appTitles.indexOf(pkfF10))

        shortcut_f7.onItemSelectedListener = this
        shortcut_f8.onItemSelectedListener = this
        shortcut_f9.onItemSelectedListener = this
        shortcut_f10.onItemSelectedListener = this
    }

    @SuppressLint("ApplySharedPref")
    override fun onItemSelected(spinner: AdapterView<*>, view: View, position: Int, arg3: Long) {
        var keycode = 0
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val pref = getSharedPreferences("utility", MODE_PRIVATE)
        val edit = pref.edit()

        when (spinner.id) {
            R.id.shortcut_f7 -> keycode = KeyEvent.KEYCODE_F7
            R.id.shortcut_f8 -> keycode = KeyEvent.KEYCODE_F8
            R.id.shortcut_f9 -> keycode = KeyEvent.KEYCODE_F9
            R.id.shortcut_f10 -> keycode = KeyEvent.KEYCODE_F10
        }

        val shortcutPref = "shortcut_f${keycode - KeyEvent.KEYCODE_F1 + 1}"

        if (position == 0) {
            wm.setApplicationShortcut(keycode, null)
            edit.putString(shortcutPref, "No shortcut")
        } else {
            wm.setApplicationShortcut(keycode, appIntentList[position - 1])
            edit.putString(shortcutPref, appIntentList[position - 1].`package`)
        }
        edit.commit()
    }

    override fun onNothingSelected(arg0: AdapterView<*>) {

    }

    private var appList: List<ApplicationInfo>? = null
    private fun getAvailableAppList(): List<Intent> {
        val pm = packageManager
        appList = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val launchApps = appList!!.mapNotNullTo(ArrayList()) { pm.getLaunchIntentForPackage(it.packageName) }

        val home = Intent(Intent.ACTION_MAIN)
        home.`package` = "home"
        launchApps.add(home)

        return launchApps
    }

}