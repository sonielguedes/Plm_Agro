package com.soniel.plmagro.core.utils

import android.util.Log
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

object IndustrialLogger {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String,
        val extra: Map<String, Any>? = null,
        val throwable: String? = null
    )

    fun i(tag: String, message: String, extra: Map<String, Any>? = null) {
        log("INFO", tag, message, extra)
    }

    fun w(tag: String, message: String, extra: Map<String, Any>? = null) {
        log("WARN", tag, message, extra)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, extra: Map<String, Any>? = null) {
        log("ERROR", tag, message, extra, throwable)
    }

    fun d(tag: String, message: String, extra: Map<String, Any>? = null) {
        // Only log debug in non-production or if explicitly enabled
        log("DEBUG", tag, message, extra)
    }

    private fun log(level: String, tag: String, message: String, extra: Map<String, Any>? = null, throwable: Throwable? = null) {
        if (!com.soniel.plmagro.core.config.AppConfig.isLoggingEnabled && level == "DEBUG") return

        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message,
            extra = extra,
            throwable = throwable?.stackTraceToString()
        )
        val json = gson.toJson(entry)
        
        when (level) {
            "INFO" -> Log.i(tag, json)
            "WARN" -> Log.w(tag, json)
            "ERROR" -> Log.e(tag, json)
            "DEBUG" -> Log.d(tag, json)
            else -> Log.v(tag, json)
        }
    }
}
