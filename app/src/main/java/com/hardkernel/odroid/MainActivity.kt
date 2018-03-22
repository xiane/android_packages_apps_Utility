package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ServiceManager
import android.os.IPowerManager
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = applicationContext

        BootINI.read {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Not found boot.ini")
                    .setMessage("Check and Format Internal FAT storage?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes") { dialog, which -> startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
                    .setNegativeButton("No", null).show()

            return@read mapOf("vout" to "hdmi", "blueLed" to "on")
        }

        button_apply_reboot.setOnClickListener {
            BootINI.modify()
            reboot()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun reboot() {
        try {
            val pm = IPowerManager.Stub.asInterface(ServiceManager
                    .getService(Context.POWER_SERVICE))
            pm.reboot(false, null, false)
        } catch (e: RemoteException) {
            Log.e(TAG, "PowerManager service died!", e)
            return
        }
    }

    override fun onResume() {
        super.onResume()

    }

    companion object {
        private val TAG = "ODROIDUtility"
        private var context: Context? = null
    }
}