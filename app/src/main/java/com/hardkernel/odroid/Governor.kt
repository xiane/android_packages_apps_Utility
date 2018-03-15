package com.hardkernel.odroid

import android.util.Log
import com.hardkernel.odroid.CPU.*

class Governor(private val TAG: String, private val cluster: Cluster) {

    val governors: Array<String>
        get() {
            return scaclingAvailable!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val current: String?
        get() {
            val governor: String? = when (cluster) {
                Cluster.Big -> SystemNode.get(SystemNode.bigGovernor)
                Cluster.Little -> SystemNode.get(SystemNode.littleGovernor)
            }
            Log.e(TAG, "current governor : $governor")
            return governor
        }

    private val scaclingAvailable: String?
        get() {
            val availableGovernors: String? = when (cluster) {
                Cluster.Big -> SystemNode.get(SystemNode.bigAvailableGovernors)
                Cluster.Little -> SystemNode.get(SystemNode.littleAvailableGovernors)
            }
            Log.e(TAG, "Current available governors : $availableGovernors")
            return availableGovernors
        }

    fun set(governor: String) {
        when (cluster) {
            Cluster.Big -> SystemNode.set(SystemNode.bigGovernor, governor)
            Cluster.Little -> SystemNode.set(SystemNode.littleGovernor, governor)
        }
        Log.e(TAG, "set governor : $governor")
    }
}
