package com.hardkernel.odroid

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class CpuActivity(private val context: Context, private val TAG: String) : AdapterView.OnItemSelectedListener {
    private var cpu: CPU? = null

    fun onCreate() {
        cpu = CPU.getCPU(TAG, CPU.Cluster.Big)

        setGovernorUI(cpu!!, R.id.spinner_big_governors)
        setFrequencyUI(cpu!!, R.id.spinner_big_freq)

        cpu = CPU.getCPU(TAG, CPU.Cluster.Little)

        setGovernorUI(cpu!!, R.id.spinner_little_governors)
        setFrequencyUI(cpu!!, R.id.spinner_little_freq)
    }

    private fun setGovernorUI(cpu: CPU, ID:Int) {
        val governorSpinner = (context as Activity).findViewById(ID) as Spinner
        val governorArray = cpu.governor.governors

        val governorAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, governorArray)
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        governorSpinner.adapter = governorAdapter
        governorSpinner.onItemSelectedListener = this
        val governor = cpu.governor.current

        if (governor != null)
            governorSpinner.setSelection(governorAdapter.getPosition(governor))
    }

    private fun setFrequencyUI(cpu: CPU, ID: Int) {
        val freqSpinner = (context as Activity).findViewById(ID) as Spinner
        val frequencyArray = cpu.frequency.frequencies

        val freqAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, frequencyArray)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        freqSpinner.adapter = freqAdapter
        freqSpinner.onItemSelectedListener = this
        val freq = cpu.frequency.scalingCurrent

        if (freq != null)
            freqSpinner.setSelection(freqAdapter.getPosition(freq))
    }

    fun onResume() {

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val governor: String
        val freq: String

        val pref = context.getSharedPreferences("utility", context.MODE_PRIVATE)
        val editor = pref.edit()

        when (parent.id) {
            R.id.spinner_big_governors -> {
                governor = parent.getItemAtPosition(position).toString()
                Log.e(TAG, "big core governor = " + governor)

                cpu = CPU.getCPU(TAG, CPU.Cluster.Big)
                cpu!!.governor.set(governor)
                editor.putString("big_governor", governor)
            }
            R.id.spinner_little_governors -> {
                governor = parent.getItemAtPosition(position).toString()
                Log.e(TAG, "little core governor = " + governor)

                cpu = CPU.getCPU(TAG, CPU.Cluster.Little)
                cpu!!.governor.set(governor)
                editor.putString("little_governor", governor)
            }
            R.id.spinner_big_freq -> {
                freq = parent.getItemAtPosition(position).toString()
                Log.e(TAG, "freq")

                cpu = CPU.getCPU(TAG, CPU.Cluster.Big)
                cpu!!.frequency.setScalingMax(freq)
                editor.putString("freq", freq)
            }
            R.id.spinner_little_freq -> {
                freq = parent.getItemAtPosition(position).toString()
                Log.e(TAG, "freq")

                cpu = CPU.getCPU(TAG, CPU.Cluster.Little)
                cpu!!.frequency.setScalingMax(freq)
                editor.putString("freq", freq)
            }
            else -> {
            }
        }
        editor.commit()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {

    }
}
