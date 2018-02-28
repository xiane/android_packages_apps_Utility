package com.hardkernel.odroid

import android.util.Log

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class Governor(tag: String, private val cluster: CPU.Cluster) {

    val governors: Array<String>
        get() {
            val available_governors = scaclingAvailable
            return available_governors!!.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        }

    // TODO Auto-generated catch block
    // TODO Auto-generated catch block
    val current: String?
        get() {
            var governor: String? = null
            try {
                val fileReader: FileReader

                when (cluster) {
                    CPU.Cluster.Big -> fileReader = FileReader(BIG_GOVERNOR_NODE)
                    CPU.Cluster.Little -> fileReader = FileReader(LITTLE_GOVERNOR_NODE)
                    else -> fileReader = FileReader(BIG_GOVERNOR_NODE)
                }

                val bufferedReader = BufferedReader(fileReader)
                governor = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, governor)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return governor
        }

    private val scaclingAvailable: String?
        get() {
            var available_governors: String? = null
            try {
                val fileReader: FileReader

                when (cluster) {
                    CPU.Cluster.Big -> fileReader = FileReader(BIG_SCALING_AVAILABLE_GOVERNORS)
                    CPU.Cluster.Little -> fileReader = FileReader(LITTLE_SCALING_AVAILABLE_GOVERNORS)
                    else -> fileReader = FileReader(BIG_SCALING_AVAILABLE_GOVERNORS)
                }

                val bufferedReader = BufferedReader(fileReader)
                available_governors = bufferedReader.readLine()
                bufferedReader.close()
                Log.e(TAG, available_governors)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return available_governors
        }

    init {
        TAG = tag
    }

    fun set(governor: String) {
        val out: BufferedWriter
        val fileWriter: FileWriter

        try {
            when (cluster) {
                CPU.Cluster.Big -> fileWriter = FileWriter(BIG_GOVERNOR_NODE)
                CPU.Cluster.Little -> fileWriter = FileWriter(LITTLE_GOVERNOR_NODE)
                else -> fileWriter = FileWriter(BIG_GOVERNOR_NODE)
            }

            out = BufferedWriter(fileWriter)
            out.write(governor)
            out.newLine()
            out.close()
            Log.e(TAG, "set governor : " + governor)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    companion object {
        /* big Cluster */
        private val BIG_GOVERNOR_NODE = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor"
        private val BIG_SCALING_AVAILABLE_GOVERNORS = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors"
        /* little Cluster */
        private val LITTLE_GOVERNOR_NODE = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor"
        private val LITTLE_SCALING_AVAILABLE_GOVERNORS = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors"

        private lateinit var TAG: String
    }
}
