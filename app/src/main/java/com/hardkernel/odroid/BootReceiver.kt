package com.hardkernel.odroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import android.os.SystemProperties
import android.view.KeyEvent
import android.view.WindowManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
            CPU.getCPU(TAG, CPU.Cluster.Big)
                    .governor.set(pref.getString("big_governor", "performance"))
            CPU.getCPU(TAG, CPU.Cluster.Little)
                    .governor.set(pref.getString("little_governor", "performance"))

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
            appList
                    .filter { it.`package` == pkg[i] }
                    .forEach { wm.setApplicationShortcut(KeyEvent.KEYCODE_F7 + i, it) }
        }
    }

    private fun setMouse(handed: String?) {
        SystemProperties.set("mouse.firstbutton", handed)
        Log.e(TAG, "set prop mouse.firstbutton " + handed!!)
    }

    companion object {

        private const val TAG = "ODROIDUtility"
        private const val autoStart = "org.xbmc.kodi"
    }
}
