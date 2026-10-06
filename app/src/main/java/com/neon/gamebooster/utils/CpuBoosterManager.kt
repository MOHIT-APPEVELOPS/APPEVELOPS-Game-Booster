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
                
                // Reflection to bypass private access of Shizuku.newProcess
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, command, null, null) as Process
                
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
