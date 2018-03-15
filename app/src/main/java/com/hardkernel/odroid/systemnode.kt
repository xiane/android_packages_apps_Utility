package com.hardkernel.odroid

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter

object SystemNode {
    private const val cpuRoot = "/sys/devices/system/cpu/cpufreq"
    private const val big = "policy4"
    private const val little = "policy0"
    private const val frequencies = "scaling_available_frequencies"
    private const val max_freq = "scaling_max_freq"
    private const val governors = "scaling_available_frequencies"
    private const val current_gov = "scaling_governor"

    /* Frequency */
    /* Big cluster */
    const val bigAvailableFreq = "$cpuRoot/$big/$frequencies"
    const val bigMaxFreq = "$cpuRoot/$big/$max_freq"
    /* Little cluster */
    const val littleAvailableFreq = "$cpuRoot/$little/$frequencies"
    const val littleMaxFreq = "$cpuRoot/$little/$max_freq"

    /* Governor */
    /* big Cluster */
    const val bigGovernor = "$cpuRoot/$big/$governors"
    const val bigAvailableGovernors = "$cpuRoot/$big/$current_gov"
    /* little Cluster */
    const val littleGovernor = "$cpuRoot/$little/$governors"
    const val littleAvailableGovernors = "$cpuRoot/$little/$current_gov"

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