package com.hardkernel.odroid

import android.util.Log
import com.hardkernel.odroid.CPU.*

class Frequency(private val TAG: String, private val cluster: Cluster) {

    val frequencies: Array<String>
        get() {
            return scalingAvailables!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val scalingCurrent: String?
        get() {
            val freq: String? = when (cluster) {
                Cluster.Big -> SystemNode.bigMaxFreq.read()
                Cluster.Little -> SystemNode.littleMaxFreq.read()
            }
            Log.e(TAG, "Current frequency : $freq")
            return freq
        }

    private val scalingAvailables: String?
        get() {
            val availableFrequencies: String? = when (cluster) {
                Cluster.Big -> SystemNode.bigAvailableFreq.read()
                Cluster.Little -> SystemNode.littleAvailableFreq.read()
            }
            Log.e(TAG, "Available Frequencies : $availableFrequencies")
            return availableFrequencies
        }

    fun setScalingMax(freq: String) {
        when (cluster) {
            Cluster.Big -> SystemNode.bigMaxFreq.write(freq)
            Cluster.Little -> SystemNode.littleMaxFreq.write(freq)
        }
        Log.e(TAG, "set freq : $freq")
    }
}
