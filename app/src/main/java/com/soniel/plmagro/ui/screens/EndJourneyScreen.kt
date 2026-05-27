package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.components.NumericKeypad
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.CardBackground
import com.soniel.plmagro.ui.theme.NeonGreen

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Share
import com.soniel.plmagro.core.utils.ShareUtils
import com.soniel.plmagro.viewmodel.JourneySummary

@Composable
fun EndJourneyScreen(
    kmInicial: Int, 
    kmAtual: Int, 
    vehicleId: String = "---",
    operatorName: String = "---",
    summary: JourneySummary? = null,
    onBack: () -> Unit, 
    onFinish: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val kmRodado = if (kmAtual > kmInicial) kmAtual - kmInicial else 0
    val durationHours = summary?.durationMillis?.let { it / (1000 * 60 * 60) } ?: 0
    val durationMinutes = summary?.durationMillis?.let { (it / (1000 * 60)) % 60 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Finalizar Jornada", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlmCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("RESUMO DE PRODUTIVIDADE", style = MaterialTheme.typography.labelSmall, color = NeonGreen, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                StatRow(Icons.Default.Timer, "Duração do Turno", "${durationHours}h ${durationMinutes}m")
                StatRow(Icons.Default.PinDrop, "Distância Total", "${"%.2f".format(summary?.distanceKm ?: 0.0)} KM")
                StatRow(Icons.Default.LocalGasStation, "Abastecimentos", "${summary?.refuelingCount ?: 0}")

                if (summary?.visitedAreas?.isNotEmpty() == true) {
                    Spacer(Modifier.height(12.dp))
                    Text("ÁREAS PERCORRIDAS:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        summary.visitedAreas.joinToString(", "),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlmCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("DADOS DO EQUIPAMENTO", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                SummaryRow("KM INICIAL", "$kmInicial")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
                SummaryRow("KM FINAL", "$kmAtual", isBold = true, valueColor = NeonGreen)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PlmButton(
            text = "CONFIRMAR ENCERRAMENTO",
            onClick = { onFinish(kmAtual) }
        )

        if (summary != null) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { 
                        ShareUtils.shareJourneyReport(context, vehicleId, operatorName, kmAtual, summary) 
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TEXTO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { 
                        ShareUtils.generateAndSharePdf(context, vehicleId, operatorName, kmAtual, summary) 
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("BOLETIM PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.LightGray, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SummaryRow(label: String, value: String, color: Color = Color.Gray, valueColor: Color = Color.White, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = color, fontSize = 12.sp)
        Text(value, fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold, color = valueColor, fontSize = 18.sp)
    }
}
