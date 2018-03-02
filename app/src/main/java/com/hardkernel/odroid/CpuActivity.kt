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

    /* Big Cluster */
    private var SpinnerBigFreq: Spinner? = null
    private var BigScalingMaxFreq: String? = null

    /* Little Cluster */
    private var SpinnerLittleFreq: Spinner? = null
    private var LittleScalingMaxFreq: String? = null

    private var cpu: CPU? = null

    fun onCreate() {
        var frequency_array: Array<String>
        var freqAdapter: ArrayAdapter<String>

        /* Big Cluster */
        cpu = CPU.getCPU(TAG, CPU.Cluster.Big)

        setGovernorUI(cpu!!, R.id.spinner_big_governors)

        /* Frequency */
        SpinnerBigFreq = context.findViewById(R.id.spinner_big_freq) as Spinner

        frequency_array = cpu!!.frequency.frequencies
        freqAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, frequency_array)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        SpinnerBigFreq!!.adapter = freqAdapter

        SpinnerBigFreq!!.onItemSelectedListener = this

        BigScalingMaxFreq = cpu!!.frequency.scalingCurrent

        if (BigScalingMaxFreq != null)
            SpinnerBigFreq!!.setSelection(freqAdapter.getPosition(BigScalingMaxFreq))

        /* Little Cluster */
        cpu = CPU.getCPU(TAG, CPU.Cluster.Little)

        setGovernorUI(cpu!!, R.id.spinner_little_governors)

        /* Frequency */
        SpinnerLittleFreq = context.findViewById(R.id.spinner_little_freq) as Spinner

        frequency_array = cpu!!.frequency.frequencies
        freqAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, frequency_array)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        SpinnerLittleFreq!!.adapter = freqAdapter

        SpinnerLittleFreq!!.onItemSelectedListener = this

        LittleScalingMaxFreq = cpu!!.frequency.scalingCurrent

        if (LittleScalingMaxFreq != null)
            SpinnerLittleFreq!!.setSelection(freqAdapter.getPosition(LittleScalingMaxFreq))
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
