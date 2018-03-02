package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class UpdateActivity(private val context: Context, private val TAG: String) {
    companion object {
        private var checkCustomServer = false
    }

    @SuppressLint("ApplySharedPref")
    fun onCreate() {
        (context as Activity).setContentView(R.layout.activity_main)
        var url: String
        val btn = context.button_update_url
        val editText = context.edittext

        fun selectServer(server:String, edit:Boolean) {
            checkCustomServer = edit
            editText.setText(server, TextView.BufferType.NORMAL)
            editText.isEnabled = edit

            btn.isEnabled = edit
            url = server

            MainActivity.ServerInfo.write(url)
            UpdatePackage.remoteUrl =url
        }

        context.rb_offical_server.setOnClickListener {
            selectServer(
                    UpdatePackage.OFFICAL_SERVER_URL,
                    false
            )
        }
        context.rb_mirror_server.setOnClickListener {
            selectServer(
                    UpdatePackage.MIRROR_SERVER_URL,
                    false
            )
        }
        context.rb_custom_server.setOnClickListener {
            val pref = context.getSharedPreferences("utility",
                    Context.MODE_PRIVATE)
            selectServer(pref.getString("custom_server", UpdatePackage.MIRROR_SERVER_URL),
                    true)
        }

        btn.setOnClickListener {
            url = editText.text.toString()

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
            UpdatePackage.remoteUrl = url
        }

        val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
        checkCustomServer = pref.getBoolean("custom_server_rb", false)

        if (checkCustomServer) {
            context.rb_custom_server.isChecked = true
            editText.setText(pref.getString("custom_server", UpdatePackage.remoteUrl),
                    TextView.BufferType.EDITABLE)

        } else {
            context.rb_mirror_server.isChecked = true
            editText.setText(UpdatePackage.remoteUrl, TextView.BufferType.EDITABLE)
            editText.isEnabled = false
        }
    }
}
