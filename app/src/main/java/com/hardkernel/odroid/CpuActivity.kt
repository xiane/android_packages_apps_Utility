package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import kotlinx.android.synthetic.main.activity_main.*
import com.hardkernel.odroid.CPU.*

class CpuActivity(private val context: Context, private val TAG: String) : AdapterView.OnItemSelectedListener {
    private lateinit var cpu: CPU

    fun onCreate() {
        (context as Activity).setContentView(R.layout.activity_main)
        cpu = CPU.getCPU(TAG, Cluster.Big)

        setGovernorUI(cpu, context.spinner_big_governors)
        setFrequencyUI(cpu, context.spinner_big_freq)

        cpu = CPU.getCPU(TAG, CPU.Cluster.Little)

        setGovernorUI(cpu, context.spinner_little_governors)
        setFrequencyUI(cpu, context.spinner_little_freq)
    }

    private fun setGovernorUI(cpu: CPU, governorSpinner:Spinner) {
        governorSpinner.setSpinner(array = cpu.governor.governors, current = cpu.governor.current)
    }

    private fun setFrequencyUI(cpu: CPU, freqSpinner:Spinner) {
        freqSpinner.setSpinner(array = cpu.frequency.frequencies, current = cpu.frequency.scalingCurrent)
    }

    private fun Spinner.setSpinner (array: Array<String>, current: String?) {
        val adapter = ArrayAdapter(this@CpuActivity.context, android.R.layout.simple_spinner_dropdown_item, array)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
        onItemSelectedListener = this@CpuActivity

        if (current != null)
            setSelection(adapter.getPosition(current))
    }

    fun onResume() {

    }

    @SuppressLint("ApplySharedPref")
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val pref = context.getSharedPreferences("utility", context.MODE_PRIVATE)
        val editor = pref.edit()

        fun setGovernor(cluster:Cluster) {
            val governor = parent.getItemAtPosition(position).toString()
            Log.e(TAG, "$cluster cluster governor = $governor")

            cpu = CPU.getCPU(TAG, cluster)
            cpu.governor.set(governor)
            editor.putString("${cluster}_governor", governor)
        }

        fun setFrequency(cluster:Cluster) {
            val freq = parent.getItemAtPosition(position).toString()
            Log.e(TAG, "$cluster cluster freq = $freq")

            cpu = CPU.getCPU(TAG, cluster)
            cpu.frequency.setScalingMax(freq)
            editor.putString("${cluster}_frequency", freq)
        }

        when (parent.id) {
            R.id.spinner_big_governors -> setGovernor(Cluster.Big)
            R.id.spinner_little_governors -> setGovernor(Cluster.Little)
            R.id.spinner_big_freq -> setFrequency(Cluster.Big)
            R.id.spinner_little_freq -> setFrequency(Cluster.Little)
            else -> {}
        }
        editor.commit()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {
    }
}