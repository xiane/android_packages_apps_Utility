package com.hardkernel.odroid

import android.util.Log

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import com.hardkernel.odroid.CPU.*

class Frequency(private val TAG: String, private val cluster: Cluster) {

    val frequencies: Array<String>
        get() {
            val availableFrequencies = scalingAvailables
            return availableFrequencies!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val scalingCurrent: String?
        get() {
            val freq: String? = when (cluster) {
                Cluster.Big -> getSystemValue(SystemNode.bigScalingMaxFreq)
                Cluster.Little -> getSystemValue(SystemNode.littleScalingMaxFreq)
            }
            Log.e(TAG, "Current frequency : $freq")
            return freq
        }

    private val scalingAvailables: String?
        get() {
            val availableFrequencies: String? = when (cluster) {
                Cluster.Big -> getSystemValue(SystemNode.bigScalingAvailableFreq)
                Cluster.Little -> getSystemValue(SystemNode.littleScalingAvailableFreq)
            }
            Log.e(TAG, "Available Frequencies : $availableFrequencies")
            return availableFrequencies
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

    fun setScalingMax(freq: String) {
        try {
            val fileWriter = when (cluster) {
                CPU.Cluster.Big -> FileWriter(SystemNode.bigScalingMaxFreq)
                CPU.Cluster.Little -> FileWriter(SystemNode.littleScalingMaxFreq)
            }

            val out = BufferedWriter(fileWriter)
            out.write(freq)
            out.newLine()
            out.close()
            Log.e(TAG, "set freq : $freq")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
