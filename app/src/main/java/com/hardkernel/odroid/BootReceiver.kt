package com.hardkernel.odroid

import java.io.OutputStream

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Debug
import android.util.Log

import android.os.SystemProperties
import android.view.KeyEvent
import android.view.WindowManager

class BootReceiver : BroadcastReceiver() {
    private val cpu_big = CPU.getCPU(TAG, CPU.Cluster.Big)
    private val cpu_little = CPU.getCPU(TAG, CPU.Cluster.Little)
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
            cpu_big.governor.set(pref.getString("big_governor", "performance"))
            cpu_little.governor.set(pref.getString("little_governor", "performance"))

            setMouse(pref.getString("mouse", "right"))

            /* Auto start application on boot */
            if (pref.getBoolean("kodi", false))
                context.startActivity(context.packageManager
                        .getLaunchIntentForPackage(autoStart))

            MainActivity.checkBootINI()
        }

        val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)

        val pkg = arrayOfNulls<String>(4)
        for (i in 0..3)
            pkg[i] = pref.getString("shortcut_f" + (i + 7), null)

        val appList = MainActivity.getAvailableAppList(context)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        for (i in 0..3) {
            for (app in appList) {
                if (app.`package` == pkg[i]) {
                    wm.setApplicationShortcut(KeyEvent.KEYCODE_F7 + i, app)
                }
            }
        }
    }

    private fun setMouse(handed: String?) {
        SystemProperties.set("mouse.firstbutton", handed)
        Log.e(TAG, "set prop mouse.firstbutton " + handed!!)
    }

    companion object {

        private val TAG = "ODROIDUtility"
        private val autoStart = "org.xbmc.kodi"
    }
}
