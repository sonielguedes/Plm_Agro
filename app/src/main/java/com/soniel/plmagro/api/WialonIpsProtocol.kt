package com.soniel.plmagro.api

import java.text.SimpleDateFormat
import java.util.*

/**
 * WialonIpsProtocol - Implementação do protocolo Wialon IPS v2.2
 * Documentação: https://sdk.wialon.com/wiki/en/sidebar/remoteapi/protocols/wialonips
 */
object WialonIpsProtocol {
    private val dateFormatter = SimpleDateFormat("ddMMyy;HHmmss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Formata uma mensagem no padrão Wialon IPS v2.2 (Data Package - #D#)
     * Formato: #D#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats;hdop;inputs;outputs;adc;ibutton;params\r\n
     */
    fun formatDataPackage(
        timestamp: Long,
        lat: Double,
        lng: Double,
        speed: Float,
        course: Float,
        altitude: Double,
        satellites: Int,
        extraParams: Map<String, Any>? = null,
        hdop: Double = 1.0,
        inputs: Int? = null,
        outputs: Int? = null,
        adc: List<Double>? = null,
        ibutton: String? = null
    ): String {
        val dateStr = dateFormatter.format(Date(timestamp))
        
        // Conversão de coordenadas para o formato Wialon (DDMM.MMMM;N/S e DDDMM.MMMM;E/W)
        val latFormatted = formatCoordinate(lat, true)
        val lngFormatted = formatCoordinate(lng, false)
        
        val speedInt = speed.toInt()
        val courseInt = course.toInt()
        val altInt = altitude.toInt()
        
        val hdopStr = if (hdop == 0.0) "" else "%.1f".format(Locale.US, hdop)
        val inputsStr = inputs?.toString() ?: ""
        val outputsStr = outputs?.toString() ?: ""
        val adcStr = adc?.joinToString(",") { "%.2f".format(Locale.US, it) } ?: ""
        val ibuttonStr = ibutton ?: ""

        val baseMsg = "#D#$dateStr;$latFormatted;$lngFormatted;$speedInt;$courseInt;$altInt;$satellites;$hdopStr;$inputsStr;$outputsStr;$adcStr;$ibuttonStr"
        
        // Adiciona parâmetros extras no final se existirem
        val paramsStr = extraParams?.entries?.joinToString(",") { (k, v) ->
            when (v) {
                is Int, is Long -> "$k:1:$v"
                is Double, is Float -> "$k:2:%.2f".format(Locale.US, (v as Number).toDouble())
                is Boolean -> "$k:1:${if (v) 1 else 0}"
                else -> "$k:3:$v" // 3 = String
            }
        }
        
        return if (paramsStr.isNullOrBlank()) "$baseMsg;\r\n" else "$baseMsg;$paramsStr\r\n"
    }

    /**
     * Formata o pacote de login
     * Formato: #L#unique_id;password\r\n
     */
    fun formatLoginPackage(uniqueId: String, password: String = "NA"): String {
        return "#L#$uniqueId;$password\r\n"
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
