package com.hardkernel.odroid

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter

object SystemNode {
    /* Frequency */
    /* Big cluster */
    const val bigAvailableFreq = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_frequencies"
    const val bigMaxFreq = "/sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq"
    /* Little cluster */
    const val littleAvailableFreq = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies"
    const val littleMaxFreq = "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"

    /* Governor */
    /* big Cluster */
    const val bigGovernor = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor"
    const val bigAvailableGovernors = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors"
    /* little Cluster */
    const val littleGovernor = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor"
    const val littleAvailableGovernors = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors"

    fun get(node:String): String? {
        return try {
            val reader = BufferedReader(FileReader(node))
            val value = reader.readLine()
            reader.close()
            value
        } catch (e:Exception) {
            e.printStackTrace()
            null
        }
    }

    fun set(node:String, value:String) {
        try {
            val writer = BufferedWriter(FileWriter(node))
            writer.write(value)
            writer.newLine()
            writer.close()
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}