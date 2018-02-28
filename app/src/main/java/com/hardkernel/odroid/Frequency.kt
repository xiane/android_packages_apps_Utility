package com.hardkernel.odroid

import android.util.Log

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class Frequency(tag: String, private val cluster: CPU.Cluster) {

    val frequencies: Array<String>
        get() {
            val available_frequencies = scalingAvailables
            return available_frequencies!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    val scalingCurrent: String?
        get() {
            var freq: String? = null
            val fileReader: FileReader

            try {
                when (cluster) {
                    CPU.Cluster.Big -> fileReader = FileReader(BIG_SCALING_MAX_FREQ)
                    CPU.Cluster.Little -> fileReader = FileReader(LITTLE_SCALING_MAX_FREQ)
                    else -> fileReader = FileReader(BIG_SCALING_MAX_FREQ)
                }

                val bufferedReader = BufferedReader(fileReader)
                freq = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, freq)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return freq
        }

    private val scalingAvailables: String?
        get() {
            var available_frequencies: String? = null
            try {
                val fileReader: FileReader

                when (cluster) {
                    CPU.Cluster.Big -> fileReader = FileReader(BIG_SCALING_AVAILABLE_FREQ)
                    CPU.Cluster.Little -> fileReader = FileReader(LITTLE_SCALING_AVAILABLE_FREQ)
                    else -> fileReader = FileReader(BIG_SCALING_AVAILABLE_FREQ)
                }

                val bufferedReader = BufferedReader(fileReader)
                available_frequencies = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, available_frequencies)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return available_frequencies
        }

    init {
        TAG = tag
    }

    fun setScalingMax(freq: String) {
        val out: BufferedWriter
        val fileWriter: FileWriter

        try {
            when (cluster) {
                CPU.Cluster.Big -> fileWriter = FileWriter(BIG_SCALING_MAX_FREQ)
                CPU.Cluster.Little -> fileWriter = FileWriter(LITTLE_SCALING_MAX_FREQ)
                else -> fileWriter = FileWriter(BIG_SCALING_MAX_FREQ)
            }

            out = BufferedWriter(fileWriter)
            out.write(freq)
            out.newLine()
            out.close()
            Log.e(TAG, "set freq : " + freq)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {
        /* Big cluster */
        private val BIG_SCALING_AVAILABLE_FREQ = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_frequencies"
        private val BIG_SCALING_MAX_FREQ = "/sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq"
        /* Little cluster */
        private val LITTLE_SCALING_AVAILABLE_FREQ = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies"
        private val LITTLE_SCALING_MAX_FREQ = "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"

        private lateinit var TAG: String
    }

}
