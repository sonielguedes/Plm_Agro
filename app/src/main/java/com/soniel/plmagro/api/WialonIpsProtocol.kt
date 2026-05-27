package com.soniel.plmagro.api

import java.text.SimpleDateFormat
import java.util.*

object WialonIpsProtocol {
    private val dateFormatter = SimpleDateFormat("ddMMyy;HHmmss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Formata uma mensagem no padrão Wialon IPS (Data Package - #D#)
     * Formato: #D#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats
     */
    fun formatDataPackage(
        imei: String,
        timestamp: Long,
        lat: Double,
        lng: Double,
        speed: Float,
        course: Float,
        altitude: Double,
        satellites: Int,
        extraParams: Map<String, Any>? = null
    ): String {
        val dateStr = dateFormatter.format(Date(timestamp))
        
        // Conversão de coordenadas para o formato Wialon (DDMM.MMMM;N/S)
        val latFormatted = formatCoordinate(lat, true)
        val lngFormatted = formatCoordinate(lng, false)
        
        val baseMsg = "#D#$dateStr;$latFormatted;$lngFormatted;${speed.toInt()};${course.toInt()};${altitude.toInt()};$satellites"
        
        // Adiciona parâmetros extras no final se existirem (Padrão Wialon IPS)
        val paramsStr = extraParams?.entries?.joinToString(",") { (k, v) ->
            when (v) {
                is Int -> "$k:1:$v"
                is Long -> "$k:1:$v"
                is Double -> "$k:2:$v"
                is Float -> "$k:2:$v"
                is Boolean -> "$k:1:${if (v) 1 else 0}"
                else -> "$k:3:$v" // 3 = String
            }
        }
        
        return if (paramsStr.isNullOrBlank()) "$baseMsg\r\n" else "$baseMsg;$paramsStr\r\n"
    }

    /**
     * Formata o pacote de login
     * Formato: #L#imei;password
     */
    fun formatLoginPackage(imei: String): String {
        return "#L#$imei;NA\r\n"
    }

    private fun formatCoordinate(coord: Double, isLat: Boolean): String {
        val absCoord = Math.abs(coord)
        val degrees = absCoord.toInt()
        val minutes = (absCoord - degrees) * 60
        
        val hemisphere = if (isLat) {
            if (coord >= 0) "N" else "S"
        } else {
            if (coord >= 0) "E" else "W"
        }
        
        val degreesFormatted = if (isLat) "%02d".format(degrees) else "%03d".format(degrees)
        val minutesFormatted = "%07.4f".format(Locale.US, minutes)
        
        return "$degreesFormatted$minutesFormatted;$hemisphere"
    }
}
