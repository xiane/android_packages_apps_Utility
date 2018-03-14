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
            var governor: String? = null
            try {
                val fileReader = when (cluster) {
                    Cluster.Big -> FileReader(SystemNode.bigGovernor)
                    Cluster.Little -> FileReader(SystemNode.littleGovernor)
                }

                val bufferedReader = BufferedReader(fileReader)
                governor = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, "current governor : $governor")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return governor
        }

    private val scaclingAvailable: String?
        get() {
            var availableGovernors: String? = null
            try {
                val fileReader = when (cluster) {
                    Cluster.Big -> FileReader(SystemNode.bigAvailableGovernors)
                    Cluster.Little -> FileReader(SystemNode.littleAvailableGovernors)
                }

                val bufferedReader = BufferedReader(fileReader)
                availableGovernors = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, "Current available governors : $availableGovernors")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return availableGovernors
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
