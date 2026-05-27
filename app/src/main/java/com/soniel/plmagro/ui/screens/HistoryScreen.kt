package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.model.Event
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.ui.theme.StatusMovement
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val events by viewModel.journeyEvents.collectAsStateWithLifecycle()
    val activeJourney by viewModel.activeJourney.collectAsStateWithLifecycle()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                "HISTÓRICO DA JORNADA",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        activeJourney?.let { journey ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DURAÇÃO ATUAL", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        val elapsed = System.currentTimeMillis() - journey.startTime
                        val h = elapsed / (1000 * 60 * 60)
                        val m = (elapsed / (1000 * 60)) % 60
                        Text("${h}h ${m}m", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DISTÂNCIA", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.2f".format(journey.accumulatedDistance / 1000.0)} KM", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("KM ATUAL", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${journey.lastKm}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum evento registrado nesta jornada", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(events) { event ->
                    val icon = when (event.type) {
                        "MOVIMENTO_INICIADO", "RETOMADA_OPERACAO", "OPERACAO" -> Icons.Default.Speed
                        "PARADA_DETECTADA", "PARADA_APONTADA", "PARADA_INICIADA" -> Icons.Default.Pause
                        "ABASTECIMENTO" -> Icons.Default.LocalGasStation
                        "OPERACAO_ALTERADA" -> Icons.Default.PlayArrow
                        "ERRO_GPS", "PERDA_GPS" -> Icons.Default.GpsOff
                        "ENTROU_NA_CERCA" -> Icons.Default.LocationOn
                        "SAIU_DA_CERCA" -> Icons.Default.LocationOff
                        "EXCESSO_VELOCIDADE" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    }

                    // Lógica de cores solicitada: Paradas em Vermelho, Produtivo em Verde
                    val color = when {
                        event.type.contains("PARADA") || event.type.contains("GPS") || event.type.contains("EXCESSO") -> Color.Red
                        event.type.contains("OPERACAO") || event.type.contains("MOVIMENTO") || event.type == "RETOMADA_OPERACAO" -> StatusMovement
                        event.type.contains("CERCA") -> Color.Yellow
                        else -> when (event.severity) {
                            1 -> StatusMovement // SUCCESS/INFO GREEN
                            2 -> Color.Yellow // WARNING
                            3 -> Color.Red // ERROR
                            else -> Color.White // DEFAULT/INFO
                        }
                    }

                    val gpsInfo = if (event.latitude != 0.0 && event.longitude != 0.0) {
                        "GPS: ${"%.4f".format(event.latitude)}, ${"%.4f".format(event.longitude)}"
                    } else {
                        "GPS: Buscando sinal..."
                    }

                    TimelineItem(
                        time = timeFormat.format(Date(event.timestamp)),
                        title = event.type.replace("_", " "),
                        description = "${event.description}\nKM: ${event.kmAtTime} | $gpsInfo",
                        icon = icon,
                        iconColor = color
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItem(time: String, title: String, description: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Text(time, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.DarkGray)
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .background(Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .weight(1f)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text(description, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
