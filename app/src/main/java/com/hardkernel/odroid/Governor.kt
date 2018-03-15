package com.hardkernel.odroid

import android.util.Log

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import com.hardkernel.odroid.CPU.*

class Governor(private val TAG: String, private val cluster: Cluster) {

    val governors: Array<String>
        get() {
            return scaclingAvailable!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val current: String?
        get() {
            val governor: String? = when (cluster) {
                Cluster.Big -> getSystemValue(SystemNode.bigGovernor)
                Cluster.Little -> getSystemValue(SystemNode.littleGovernor)
            }
            Log.e(TAG, "current governor : $governor")
            return governor
        }

    private val scaclingAvailable: String?
        get() {
            val availableGovernors: String? = when (cluster) {
                Cluster.Big -> getSystemValue(SystemNode.bigAvailableGovernors)
                Cluster.Little -> getSystemValue(SystemNode.littleAvailableGovernors)
            }
            Log.e(TAG, "Current available governors : $availableGovernors")
            return availableGovernors
        }

    private fun getSystemValue(node:String): String? {
        return try {
            val reader =BufferedReader(FileReader(node))
            val value = reader.readLine()
            reader.close()
            value
        } catch (e:Exception) {
            e.printStackTrace()
            null
        }
    }

    fun set(governor: String) {
        try {
            val fileWriter = when (cluster) {
                Cluster.Big -> FileWriter(SystemNode.bigGovernor)
                Cluster.Little -> FileWriter(SystemNode.littleGovernor)
            }

            val out = BufferedWriter(fileWriter)
            out.write(governor)
            out.newLine()
            out.close()
            Log.e(TAG, "set governor : $governor")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
