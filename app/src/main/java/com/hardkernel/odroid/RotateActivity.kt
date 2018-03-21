package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import kotlinx.android.synthetic.main.rotation_activity.*

@SuppressLint("Registered")
class RotateActivity:Activity() {
    private var degree: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val display = windowManager.defaultDisplay
        degree = display.rotation * 90

        when (degree) {
            0 -> {
                radio_landscape.isChecked = true
                radioGroup_degree.visibility = View.GONE
            }
            else -> {
                radio_portrait.isChecked = true
                radioGroup_degree.visibility = View.VISIBLE

                when (degree) {
                    90 -> {
                        radio_90.isChecked = true
                        radio_270.isChecked = false

                    }
                    else -> {
                        radio_90.isChecked = false
                        radio_270.isChecked = true
                    }
                }
            }
        }

        radio_portrait.setOnClickListener {_ ->
            radioGroup_degree.visibility = View.VISIBLE
            degree = 270
            radio_90.isChecked = false
            radio_270.isChecked = true
        }

        radio_landscape.setOnClickListener {_ ->
            radioGroup_degree.visibility = View.GONE
            degree = 0
        }

        radio_90.setOnClickListener {_ -> degree = 90 }

        radio_270.setOnClickListener {_ -> degree = 270 }

        button_rotation_apply.setOnClickListener {_ ->
            when (degree) {
                0 -> {
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 0)
                }
                90 -> {
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 1)
                }
                270 -> {
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 3)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        radioGroup_degree.visibility = if (radio_portrait.isChecked) View.VISIBLE else View.GONE
    }
}