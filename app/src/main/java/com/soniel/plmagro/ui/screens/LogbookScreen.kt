package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import com.soniel.plmagro.core.utils.ShareUtils
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.model.Journey
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.JourneySummary
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogbookScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val journeys by viewModel.historicalJourneys.collectAsStateWithLifecycle()
    val activeVinculo by viewModel.activeVinculo.collectAsStateWithLifecycle()
    val config by viewModel.vehicleConfig.collectAsStateWithLifecycle()
    
    val vehicleId = activeVinculo?.codigoFrotaLocal ?: config?.fleetCode ?: "---"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Black)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Ficha de Trabalho", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        
        Text(
            "Histórico das últimas 30 jornadas", 
            color = Color.Gray, 
            modifier = Modifier.padding(start = 48.dp, bottom = 24.dp),
            fontSize = 12.sp
        )

        if (journeys.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                    Text("Nenhuma jornada finalizada encontrada.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(journeys) { journey ->
                    HistoricalJourneyItem(journey, viewModel, vehicleId)
                }
            }
        }
    }
}

@Composable
fun HistoricalJourneyItem(journey: Journey, viewModel: MainViewModel, vehicleId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var summary by remember { mutableStateOf<JourneySummary?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM (EEE)", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(journey.id) {
        summary = viewModel.getSummaryForJourney(journey)
    }

    PlmCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(journey.startTime)).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 14.sp
                    )
                    val startStr = timeFormat.format(Date(journey.startTime))
                    val endStr = journey.endTime?.let { timeFormat.format(Date(it)) } ?: "--:--"
                    Text(
                        text = "$startStr até $endStr",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    summary?.let { s ->
                        IconButton(
                            onClick = { 
                                ShareUtils.shareJourneyReport(
                                    context, 
                                    vehicleId, 
                                    "Matrícula: ${journey.operatorMatricula}", 
                                    journey.kmFinal ?: journey.lastKm, 
                                    s
                                ) 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = NeonGreen, modifier = Modifier.size(20.dp))
                        }
                        
                        IconButton(
                            onClick = { 
                                ShareUtils.generateAndSharePdf(
                                    context, 
                                    vehicleId, 
                                    "Matrícula: ${journey.operatorMatricula}", 
                                    journey.kmFinal ?: journey.lastKm, 
                                    s
                                ) 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    Surface(
                        color = Color.DarkGray,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "ID #${journey.id}", 
                            color = Color.White, 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("KM FINAL", color = Color.Gray, fontSize = 10.sp)
                    Text("${journey.kmFinal ?: journey.lastKm}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HORÍMETRO", color = Color.Gray, fontSize = 10.sp)
                    Text("${"%.2f".format(journey.lastHorimetro)}h", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DURAÇÃO", color = Color.Gray, fontSize = 10.sp)
                    val h = summary?.durationMillis?.let { it / (1000 * 60 * 60) } ?: 0
                    val m = summary?.durationMillis?.let { (it / (1000 * 60)) % 60 } ?: 0
                    Text("${h}h ${m}m", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("OP: ${journey.operationCode}", color = Color.LightGray, fontSize = 11.sp)
                Text("CC: ${journey.costCenter}", color = Color.LightGray, fontSize = 11.sp)
            }

            if (summary?.visitedAreas?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                val areas = summary?.visitedAreas?.take(3)?.joinToString(", ") ?: ""
                val more = if ((summary?.visitedAreas?.size ?: 0) > 3) "..." else ""
                Text("LOCAIS: $areas$more", color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}
