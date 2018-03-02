package com.hardkernel.odroid

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import kotlinx.android.synthetic.main.activity_main.*

class CpuActivity(private val context: Context, private val TAG: String) : AdapterView.OnItemSelectedListener {
    private var cpu: CPU? = null

    fun onCreate() {
        (context as Activity).setContentView(R.layout.activity_main)
        cpu = CPU.getCPU(TAG, CPU.Cluster.Big)

        setGovernorUI(cpu!!, context.spinner_big_governors)
        setFrequencyUI(cpu!!, context.spinner_big_freq)

        cpu = CPU.getCPU(TAG, CPU.Cluster.Little)

        setGovernorUI(cpu!!, context.spinner_little_governors)
        setFrequencyUI(cpu!!, context.spinner_little_freq)
    }

    private fun setGovernorUI(cpu: CPU, governorSpinner:Spinner) {
        val governorArray = cpu.governor.governors

        val governorAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, governorArray)
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        governorSpinner.adapter = governorAdapter
        governorSpinner.onItemSelectedListener = this
        val governor = cpu.governor.current

        if (governor != null)
            governorSpinner.setSelection(governorAdapter.getPosition(governor))
    }

    private fun setFrequencyUI(cpu: CPU, freqSpinner:Spinner) {
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
        val pref = context.getSharedPreferences("utility", context.MODE_PRIVATE)
        val editor = pref.edit()

        fun setGovernor(cluster: CPU.Cluster) {
            val governor = parent.getItemAtPosition(position).toString()
            Log.e(TAG, "${cluster.toString()} cluster governor = $governor")

            cpu = CPU.getCPU(TAG, cluster)
            cpu!!.governor.set(governor)
            editor.putString("${cluster.toString()}_governor", governor)
        }

        fun setFrequency(cluster: CPU.Cluster) {
            val freq = parent.getItemAtPosition(position).toString()
            Log.e(TAG, "${cluster.toString()} cluster freq = $freq")

            cpu = CPU.getCPU(TAG, cluster)
            cpu!!.frequency.setScalingMax(freq)
            editor.putString("${cluster.toString()}_frequency", freq)
        }

        when (parent.id) {
            R.id.spinner_big_governors -> setGovernor(CPU.Cluster.Big)
            R.id.spinner_little_governors -> setGovernor(CPU.Cluster.Little)
            R.id.spinner_big_freq -> setFrequency(CPU.Cluster.Big)
            R.id.spinner_little_freq -> setFrequency(CPU.Cluster.Little)
            else -> {}
        }
        editor.commit()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {

    }
}