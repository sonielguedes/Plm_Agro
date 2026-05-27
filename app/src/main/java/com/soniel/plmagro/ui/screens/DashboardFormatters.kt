package com.soniel.plmagro.ui.screens

import kotlin.math.roundToInt

data class StorageDisplay(
    val percent: String,
    val detail: String
)

object DashboardFormatters {
    fun formatJourneySince(startTimeMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (startTimeMillis == null || startTimeMillis <= 0L || nowMillis <= startTimeMillis) return "---"
        val elapsedMinutes = (nowMillis - startTimeMillis) / 60_000L
        val hours = elapsedMinutes / 60L
        val minutes = elapsedMinutes % 60L
        return "%02dh%02d".format(hours, minutes)
    }

    fun formatStorage(freeDiskMb: Long, totalDiskMb: Long): StorageDisplay {
        if (freeDiskMb <= 0L || totalDiskMb <= 0L || freeDiskMb > totalDiskMb) {
            val detail = if (freeDiskMb > 0L) "$freeDiskMb MB livre" else "---"
            return StorageDisplay(percent = "--%", detail = detail)
        }
        val usedMb = totalDiskMb - freeDiskMb
        val usedPercent = ((usedMb.toDouble() / totalDiskMb.toDouble()) * 100.0).roundToInt()
        return StorageDisplay("${usedPercent.coerceIn(0, 100)}%", "$usedMb MB / $totalDiskMb MB")
    }

    fun isHeartbeatOk(lastHeartbeatTime: Long, nowMillis: Long = System.currentTimeMillis(), timeoutMillis: Long = 120_000L): Boolean {
        return lastHeartbeatTime > 0L && nowMillis - lastHeartbeatTime <= timeoutMillis
    }

    fun formatHours(hours: Double?): String {
        return if (hours == null) "-- h" else "%.1f h".format(hours).replace('.', ',')
    }

    fun formatTemperature(tempCelsius: Float): String {
        return if (tempCelsius > 0f) "${tempCelsius.roundToInt()}°C" else "--°C"
    }
}
