package com.hardkernel.odroid

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import kotlinx.android.synthetic.main.cpu_activity.*
import com.hardkernel.odroid.CPU.*

@SuppressLint("Registered")
class CpuActivity:Activity(), AdapterView.OnItemSelectedListener {
    private val tag="ODROIDUtility"
    private lateinit var cpu: CPU

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cpu_activity)

        cpu = CPU.getCPU(tag, Cluster.Big)

        setGovernorUI(cpu, spinner_big_governors)
        setFrequencyUI(cpu, spinner_big_freq)

        cpu = CPU.getCPU(tag, CPU.Cluster.Little)

        setGovernorUI(cpu, spinner_little_governors)
        setFrequencyUI(cpu, spinner_little_freq)
    }

    private fun setGovernorUI(cpu: CPU, governorSpinner:Spinner) {
        governorSpinner.setSpinner(array = cpu.governor.governors, current = cpu.governor.current)
    }

    private fun setFrequencyUI(cpu: CPU, freqSpinner:Spinner) {
        freqSpinner.setSpinner(array = cpu.frequency.frequencies, current = cpu.frequency.scalingCurrent)
    }

    private fun Spinner.setSpinner (array: Array<String>, current: String?) {
        val adapter = ArrayAdapter(this@CpuActivity, android.R.layout.simple_spinner_dropdown_item, array)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
        onItemSelectedListener = this@CpuActivity

        if (current != null)
            setSelection(adapter.getPosition(current))
    }

    @SuppressLint("ApplySharedPref")
    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val pref = getSharedPreferences("utility", MODE_PRIVATE)
        val editor = pref.edit()

        fun setGovernor(cluster:Cluster) {
            val governor = parent.getItemAtPosition(position).toString()
            Log.e(tag, "$cluster cluster governor = $governor")

            cpu = CPU.getCPU(tag, cluster)
            cpu.governor.set(governor)
            editor.putString("${cluster}_governor", governor)
        }

        fun setFrequency(cluster:Cluster) {
            val freq = parent.getItemAtPosition(position).toString()
            Log.e(tag, "$cluster cluster freq = $freq")

            cpu = CPU.getCPU(tag, cluster)
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