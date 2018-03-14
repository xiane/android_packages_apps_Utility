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
            var freq: String? = null
            try {
                val fileReader = when (cluster) {
                    CPU.Cluster.Big -> FileReader(SystemNode.bigScalingMaxFreq)
                    CPU.Cluster.Little -> FileReader(SystemNode.littleScalingMaxFreq)
                }

                val bufferedReader = BufferedReader(fileReader)
                freq = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, freq)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return freq
        }

    private val scalingAvailables: String?
        get() {
            var availableFrequencies: String? = null
            try {
                val fileReader = when (cluster) {
                    CPU.Cluster.Big -> FileReader(SystemNode.bigScalingAvailableFreq)
                    CPU.Cluster.Little -> FileReader(SystemNode.littleScalingAvailableFreq)
                }

                val bufferedReader = BufferedReader(fileReader)
                availableFrequencies = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, availableFrequencies)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return availableFrequencies
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
