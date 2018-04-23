package com.hardkernel.odroid

import android.util.Log
import com.hardkernel.odroid.CPU.*

class Governor(private val TAG: String, private val cluster: Cluster) {

    val governors: Array<String>
        get() {
            return scalingAvailables!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val current: String?
        get() {
            val governor: String? = when (cluster) {
                Cluster.Big -> SystemNode.bigGovernor.read()
                Cluster.Little -> SystemNode.littleGovernor.read()
            }
            Log.e(TAG, "current governor : $governor")
            return governor
        }

    private val scalingAvailables: String?
        get() {
            val availableGovernors: String? = when (cluster) {
                Cluster.Big -> SystemNode.bigAvailableGovernors.read()
                Cluster.Little -> SystemNode.littleAvailableGovernors.read()
            }
            Log.e(TAG, "Current available governors : $availableGovernors")
            return availableGovernors
        }

    fun set(governor: String) {
        when (cluster) {
            Cluster.Big -> SystemNode.bigGovernor.write(governor)
            Cluster.Little -> SystemNode.littleGovernor.write(governor)
        }
        Log.e(TAG, "set governor : $governor")
    }
}
