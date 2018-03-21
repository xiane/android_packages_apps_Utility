package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.board_activity.*

@SuppressLint("Registered")
class BoardActivity:Activity() {
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cb_kodi.setOnCheckedChangeListener { _, isChecked ->
            val pref = getSharedPreferences("utility", MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("kodi", isChecked)
            editor.commit()
        }
        var blueLed = BootINI.get("BlueLed")!!

        blue_led.isChecked = blueLed == "on"
        blue_led.setText(if (blueLed == "on") R.string.on else R.string.off)

        blue_led.setOnCheckedChangeListener { _, isChecked ->
            blueLed = if (isChecked) "on" else "off"
            blue_led.setText(if (isChecked) R.string.on else R.string.off)
            BootINI.updateOptions(mapOf("blueLed" to blueLed))
            BootINI.modify()
        }
    }

    override fun onResume() {
        super.onResume()

        val pref = getSharedPreferences("utility", MODE_PRIVATE)
        cb_kodi.isChecked = pref.getBoolean("kodi", false)
    }

}