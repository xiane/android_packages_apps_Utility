package com.hardkernel.odroid

class CPU private constructor(tag: String, cluster: Cluster) {
    var governor: Governor = Governor(tag, cluster)
    var frequency: Frequency = Frequency(tag, cluster)

    enum class Cluster {
        Big,
        Little;

        override fun toString(): String {
            return when (this) {
                Big -> "big"
                Little -> "little"
            }
        }
    }

    companion object {
        private lateinit var tag: String
        private val cpu_big: CPU by lazy { CPU(tag, Cluster.Big) }
        private val cpu_little: CPU by lazy { CPU(tag, Cluster.Little) }

        fun getCPU(tag: String, cluster: Cluster): CPU {
            this.tag = tag
            return when (cluster) {
                CPU.Cluster.Big -> cpu_big
                CPU.Cluster.Little -> cpu_little
            }
        }
    }
}