package com.soniel.plmagro.core.utils

import android.content.Context
import android.content.Intent
import com.soniel.plmagro.viewmodel.JourneySummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtils {

    fun shareJourneyReport(
        context: Context,
        vehicleId: String,
        operatorName: String,
        kmFinal: Int,
        summary: JourneySummary
    ) {
        val h = summary.durationMillis / (1000 * 60 * 60)
        val m = (summary.durationMillis / (1000 * 60)) % 60
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val reportText = """
            *🚩 BOLETIM DE OPERAÇÃO - PLMAGRO*
            ------------------------------------------
            🗓 *Data:* $dateStr
            🚜 *Equipamento:* $vehicleId
            👤 *Operador:* $operatorName
            ------------------------------------------
            ⏱ *Duração do Turno:* ${h}h ${m}m
            🛣 *KM Final:* $kmFinal
            📏 *Distância Rodada:* ${"%.2f".format(summary.distanceKm)} KM
            ⛽ *Abastecimentos:* ${summary.refuelingCount}
            
            📍 *Áreas de Trabalho:*
            ${if (summary.visitedAreas.isEmpty()) "Nenhuma área registrada" else summary.visitedAreas.joinToString("\n• ", prefix = "• ")}
            ------------------------------------------
            _Gerado automaticamente via PLMAGRO Telemetria_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        
        val shareIntent = Intent.createChooser(intent, "Enviar Boletim via:")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
