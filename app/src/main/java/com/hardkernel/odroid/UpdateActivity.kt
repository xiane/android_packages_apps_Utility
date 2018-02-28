package com.hardkernel.odroid

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView

class UpdateActivity(private val context: Context, private val TAG: String) : View.OnClickListener {

    private var editText: EditText? = null
    private var rbOfficalServer: RadioButton? = null
    private var rbMirrorServer: RadioButton? = null
    private var rbCustomServer: RadioButton? = null

    fun onCreate() {
        editText = (context as Activity).findViewById(R.id.edittext) as EditText

        rbOfficalServer = context.findViewById(R.id.rb_offical_server) as RadioButton
        rbMirrorServer = context.findViewById(R.id.rb_mirror_server) as RadioButton
        rbCustomServer = context.findViewById(R.id.rb_custom_server) as RadioButton

        rbOfficalServer!!.setOnClickListener(this)
        rbMirrorServer!!.setOnClickListener(this)
        rbCustomServer!!.setOnClickListener(this)

        val btn = context.findViewById(R.id.button_update_url) as Button
        btn.setOnClickListener(this)

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

    override fun onClick(v: View) {
        var url = String()
        when (v.id) {
            R.id.rb_offical_server -> {
                checkCustomServer = false

                editText!!.setText(UpdatePackage.OFFICAL_SERVER_URL,
                        TextView.BufferType.NORMAL)
                editText!!.isEnabled = false

                val btn = (context as Activity).findViewById(R.id.button_update_url) as Button
                btn.isEnabled = false

                url = UpdatePackage.OFFICAL_SERVER_URL
            }
            R.id.rb_mirror_server -> {
                checkCustomServer = false

                editText!!.setText(UpdatePackage.MIRROR_SERVER_URL,
                        TextView.BufferType.NORMAL)
                editText!!.isEnabled = false

                val btn = (context as Activity).findViewById(R.id.button_update_url) as Button
                btn.isEnabled = false

                url = UpdatePackage.MIRROR_SERVER_URL
            }
            R.id.rb_custom_server -> {
                checkCustomServer = true

                val pref = context.getSharedPreferences("utility", Context.MODE_PRIVATE)
                editText!!.setText(pref.getString("custom_server",
                        UpdatePackage.MIRROR_SERVER_URL),
                        TextView.BufferType.EDITABLE)
                editText!!.isEnabled = true

                val btn = (context as Activity).findViewById(R.id.button_update_url) as Button
                btn.isEnabled = true

                url = editText!!.text.toString()
            }
            R.id.button_update_url -> {
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
            }
            else -> {
            }
        }

        MainActivity.ServerInfo.write(url)
        UpdatePackage.setRemoteUrl(url)
    }

    companion object {

        private var checkCustomServer = false
    }
}
