package com.hardkernel.odroid

object SystemNode {
    /* Frequency */
    /* Big cluster */
    const val bigScalingAvailableFreq = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_frequencies"
    const val bigScalingMaxFreq = "/sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq"
    /* Little cluster */
    const val littleScalingAvailableFreq = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies"
    const val littleScalingMaxFreq = "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq"

    /* Governor */
    /* big Cluster */
    const val bigGovernor = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor"
    const val bigAvailableGovernors = "/sys/devices/system/cpu/cpufreq/policy4/scaling_available_governors"
    /* little Cluster */
    const val littleGovernor = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor"
    const val littleAvailableGovernors = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors"
}