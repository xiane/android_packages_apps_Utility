package com.hardkernel.odroid

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView

class UpdateActivity(private val context: Context, private val TAG: String) {

    private var editText: EditText? = null
    private var rbOfficalServer: RadioButton? = null
    private var rbMirrorServer: RadioButton? = null
    private var rbCustomServer: RadioButton? = null

    companion object {
        private var checkCustomServer = false
    }

    fun onCreate() {
        editText = (context as Activity).findViewById(R.id.edittext) as EditText

        rbOfficalServer = context.findViewById(R.id.rb_offical_server) as RadioButton
        rbMirrorServer = context.findViewById(R.id.rb_mirror_server) as RadioButton
        rbCustomServer = context.findViewById(R.id.rb_custom_server) as RadioButton

        var url: String
        val btn = context.findViewById(R.id.button_update_url) as Button

        fun selectServer(server:String, edit:Boolean) {
            checkCustomServer = edit
            editText!!.setText(server, TextView.BufferType.NORMAL)
            editText!!.isEnabled = edit

            btn.isEnabled = edit
            url = server

            MainActivity.ServerInfo.write(url)
            UpdatePackage.setRemoteUrl(url)
        }

        rbOfficalServer!!.setOnClickListener() {
            selectServer(UpdatePackage.OFFICAL_SERVER_URL, false)
        }
        rbMirrorServer!!.setOnClickListener() {
            selectServer(UpdatePackage.MIRROR_SERVER_URL, false)
        }
        rbCustomServer!!.setOnClickListener() {
            val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
            selectServer(pref.getString("custom_server", UpdatePackage.MIRROR_SERVER_URL), true)
        }

        btn.setOnClickListener() {
            url = editText!!.text.toString()

            val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
            val editor = pref.edit()

            if (checkCustomServer) {
                editor.putString("custom_server", url)
                editor.putBoolean("custom_server_rb", true)
            } else {
                editor.putBoolean("custom_server_rb", false)
            }
            editor.commit()

            MainActivity.ServerInfo.write(url)
            UpdatePackage.setRemoteUrl(url)
        }

        val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
        checkCustomServer = pref.getBoolean("custom_server_rb", false)

        if (checkCustomServer) {
            rbCustomServer!!.isChecked = true
            editText!!.setText(pref.getString("custom_server", UpdatePackage.remoteUrl()),
                    TextView.BufferType.EDITABLE)

        } else {
            rbMirrorServer!!.isChecked = true
            editText!!.setText(UpdatePackage.remoteUrl(), TextView.BufferType.EDITABLE)
            editText!!.isEnabled = false
        }
    }
}
