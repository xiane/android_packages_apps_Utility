package com.hardkernel.odroid

class CPU private constructor(tag: String, internal val cluster: Cluster) {
    var governor: Governor
    var frequency: Frequency

    enum class Cluster {
        Big,
        Little
    }

    init {
        governor = Governor(tag, cluster)
        frequency = Frequency(tag, cluster)
    }

    companion object {

        private var cpu_big: CPU? = null
        private var cpu_little: CPU? = null

        fun getCPU(tag: String, cluster: Cluster): CPU {
            var cpu: CPU? = null
            when (cluster) {
                CPU.Cluster.Big -> {
                    if (cpu_big == null)
                        cpu_big = CPU(tag, cluster)
                    cpu = cpu_big
                }
                CPU.Cluster.Little -> {
                    if (cpu_little == null)
                        cpu_little = CPU(tag, cluster)
                    cpu = cpu_little
                }
            }

            return cpu!!
        }
    }
}
