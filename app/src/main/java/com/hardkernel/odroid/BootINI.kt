package com.hardkernel.odroid

import android.os.SystemProperties
import android.util.Log
import java.io.*
import java.util.ArrayList

class BootINI {
    companion object {
        private const val tag = "ODROIDUtility"
        private const val bootINI = "/storage/internal/boot.ini"

        fun getOption(notifier: ()->Map<String,String> ): Map<String, String> {
            val file = File(bootINI)
            return if (file.exists()) {
                getOptions(file)
            } else {
                Log.e(tag, "Not found $bootINI")
                notifier()
            }
        }

        private fun getOptions(file:File):Map<String, String>{
            val reader = file.bufferedReader()
            var vout = ""
            var blueLed = ""
            reader.forEachLine { line ->
                when {
                    line.startsWith("setenv vout") -> {
                        vout = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""))
                        Log.e(tag, "vout : $vout")
                    }
                    line.startsWith("setenv led_onoff") -> {
                        blueLed = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""))
                        Log.e(tag, "blue led : $blueLed")
                    }
                    line.startsWith("setenv ") -> Log.e(tag, line.removePrefix("setenv "))
                }
            }

            if (vout == "") vout = "hdmi"
            if (blueLed == "") blueLed = "on"

            reader.close()
            return mapOf(
                    "vout" to vout,
                    "blueLed" to blueLed
            )
        }

        fun modify(params: Map<String, String>) {
            val vout = "setenv vout \"hdmi\""
            val blueled = "setenv led_onoff \"${params["blueLed"]}\""
            val lines = ArrayList<String>()

            val file = File(bootINI)
            val input = file.bufferedReader()

            input.forEachLine { inputLine ->
                val line = when {
                    inputLine.startsWith("setenv vout") -> vout
                    inputLine.startsWith("setenv led_onoff") -> blueled
                    else -> inputLine
                }
                Log.e(tag, line)
                lines.add("$line\n")
            }
            input.close()

            val out = file.bufferedWriter()
            for (line in lines)
                out.write(line)
            out.flush()
            out.close()

            Log.e(tag, "Update boot.ini")
        }

        fun check() {
            val file = File(bootINI)
            if (!file.exists()) SystemProperties.set("ctl.start", "makebootini")
        }
    }
}