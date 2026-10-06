package com.neon.gamebooster.utils

import rikka.shizuku.Shizuku

object CpuBoosterManager {
    fun applyPerformanceMode(): Boolean {
        return if (Shizuku.pingBinder()) {
            try {
                val command = arrayOf(
                    "sh", "-c",
                    "echo performance > /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor"
                )
                val process = Shizuku.newProcess(command, null, null)
                process.waitFor()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }
}
