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
        setContentView(R.layout.rotation_activity)

        val display = windowManager.defaultDisplay
        degree = display.rotation

        when (degree) {
            0 -> {
                radio_landscape.isChecked = true
                radioGroup_degree.visibility = View.GONE
            }
            else -> {
                radio_portrait.isChecked = true
                radioGroup_degree.visibility = View.VISIBLE

                when (degree) {
                    1 -> {
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
            degree = 3
            radio_90.isChecked = false
            radio_270.isChecked = true
        }

        radio_landscape.setOnClickListener {_ ->
            radioGroup_degree.visibility = View.GONE
            degree = 0
        }

        radio_90.setOnClickListener {_ -> degree = 1 }

        radio_270.setOnClickListener {_ -> degree = 3 }

        button_rotation_apply.setOnClickListener {_ ->
            android.provider.Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            android.provider.Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, degree)
        }
    }

    override fun onResume() {
        super.onResume()
        radioGroup_degree.visibility = if (radio_portrait.isChecked) View.VISIBLE else View.GONE
    }
}