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
                Cluster.Big -> SystemNode.get(SystemNode.bigMaxFreq)
                Cluster.Little -> SystemNode.get(SystemNode.littleMaxFreq)
            }
            Log.e(TAG, "Current frequency : $freq")
            return freq
        }

    private val scalingAvailables: String?
        get() {
            val availableFrequencies: String? = when (cluster) {
                Cluster.Big -> SystemNode.get(SystemNode.bigAvailableFreq)
                Cluster.Little -> SystemNode.get(SystemNode.littleAvailableFreq)
            }
            Log.e(TAG, "Available Frequencies : $availableFrequencies")
            return availableFrequencies
        }

    fun setScalingMax(freq: String) {
        when (cluster) {
            Cluster.Big -> SystemNode.set(SystemNode.bigMaxFreq, freq)
            Cluster.Little -> SystemNode.set(SystemNode.littleMaxFreq, freq)
        }
        Log.e(TAG, "set freq : $freq")
    }
}
