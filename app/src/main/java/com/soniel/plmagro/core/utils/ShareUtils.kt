package com.soniel.plmagro.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.soniel.plmagro.viewmodel.JourneySummary
import java.io.File
import java.io.FileOutputStream
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
            ⚙️ *Horímetro:* ${"%.2f".format(summary.horimetroFinal).replace('.', ',')}
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

    fun generateAndSharePdf(
        context: Context,
        vehicleId: String,
        operatorName: String,
        kmFinal: Int,
        summary: JourneySummary
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 50f
        
        // Header
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("PLMAGRO - RELATÓRIO OPERACIONAL", 50f, y, paint)
        
        y += 40f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Data de Geração: $dateStr", 50f, y, paint)
        
        y += 30f
        paint.isFakeBoldText = true
        canvas.drawText("EQUIPAMENTO: $vehicleId", 50f, y, paint)
        y += 20f
        canvas.drawText("OPERADOR: $operatorName", 50f, y, paint)
        
        y += 40f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(45f, y - 20f, 550f, y + 150f, paint)
        paint.style = Paint.Style.FILL
        
        y += 10f
        paint.isFakeBoldText = false
        val h = summary.durationMillis / (1000 * 60 * 60)
        val m = (summary.durationMillis / (1000 * 60)) % 60
        canvas.drawText("Duração do Turno: ${h}h ${m}m", 60f, y, paint)
        y += 25f
        canvas.drawText("KM Final: $kmFinal", 60f, y, paint)
        y += 25f
        canvas.drawText("Horímetro: ${"%.2f".format(summary.horimetroFinal).replace('.', ',')}", 60f, y, paint)
        y += 25f
        canvas.drawText("Distância Rodada: ${"%.2f".format(summary.distanceKm)} KM", 60f, y, paint)
        y += 25f
        canvas.drawText("Abastecimentos: ${summary.refuelingCount}", 60f, y, paint)
        
        if (summary.visitedAreas.isNotEmpty()) {
            y += 40f
            paint.isFakeBoldText = true
            canvas.drawText("ÁREAS DE TRABALHO:", 50f, y, paint)
            paint.isFakeBoldText = false
            y += 20f
            summary.visitedAreas.forEach { area ->
                canvas.drawText("• $area", 70f, y, paint)
                y += 20f
            }
        }
        
        y = 800f
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("Documento gerado automaticamente pelo sistema PLMAGRO Industrial.", 50f, y, paint)

        pdfDocument.finishPage(page)

        val fileName = "Relatorio_${vehicleId}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val shareIntent = Intent.createChooser(intent, "Compartilhar PDF:")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
