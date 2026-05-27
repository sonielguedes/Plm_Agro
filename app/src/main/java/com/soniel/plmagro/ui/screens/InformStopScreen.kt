package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.CardBackground
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.soniel.plmagro.model.Journey
import kotlinx.coroutines.launch

@Composable
fun InformStopScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToRefueling: () -> Unit,
    onShiftChangeComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeJourney by viewModel.activeJourney.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val vinculo by viewModel.activeVinculo.collectAsState()
    
    var selectedType by remember { mutableStateOf<String?>(null) }
    var observation by remember { mutableStateOf("") }
    
    // Padronização Industrial: ID interno (sem acento) -> Label de Exibição
    val stopTypes = remember {
        listOf(
            "ABASTECIMENTO" to "ABASTECIMENTO",
            "MECANICA" to "MECÂNICA",
            "CLIMA" to "CLIMA",
            "REFEICAO" to "REFEIÇÃO",
            "AGUARDANDO_TRANSBORDO" to "AGUARDANDO TRANSBORDO",
            "SEM_OPERADOR" to "SEM OPERADOR",
            "TROCA_DE_TURNO" to "TROCA DE TURNO",
            "AGUARDANDO_AREA" to "AGUARDANDO ÁREA",
            "COMBOIO" to "COMBOIO",
            "MANUTENCAO_PREVENTIVA" to "MANUTENÇÃO PREVENTIVA",
            "LIMPEZA_MAQUINA" to "LIMPEZA MÁQUINA",
            "DESLOCAMENTO" to "DESLOCAMENTO",
            "AJUSTE_IMPLEMENTO" to "AJUSTE IMPLEMENTO",
            "AGUARDANDO_INSUMO" to "AGUARDANDO INSUMO",
            "PROBLEMA_PNEU" to "PROBLEMA PNEU",
            "TREINAMENTO_DDS" to "TREINAMENTO DDS",
            "OUTROS" to "OUTROS"
        )
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val currentTime = remember { dateFormat.format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header Modal-like
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "APONTAMENTO DE PARADA",
                style = MaterialTheme.typography.titleMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Panel (Read-only)
        PlmCard(containerColor = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("OPERADOR", activeJourney?.operatorMatricula ?: "---")
                    InfoItem("HORA", currentTime)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("KM", activeJourney?.lastKm?.toString() ?: "0")
                    InfoItem("HORÍMETRO", "%.2f".format(activeJourney?.lastHorimetro ?: 0.0))
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoItem("GPS", currentLocation?.let { "${"%.4f".format(it.first)}, ${"%.4f".format(it.second)}" } ?: "SEM SINAL")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("MOTIVO DA PARADA (OBRIGATÓRIO)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        // Selection Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(stopTypes) { (id, label) ->
                StopTypeItem(
                    label = label,
                    isSelected = selectedType == id,
                    onClick = { selectedType = id }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Observation
        PlmTextField(
            value = observation,
            onValueChange = { observation = it },
            label = "OBSERVAÇÃO (OPCIONAL)"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm Button
        Button(
            onClick = {
                selectedType?.let { type ->
                    val desc = if (observation.isBlank()) type else "$type: $observation"
                    val km = activeJourney?.lastKm ?: 0
                    
                    viewModel.registerEvent("PARADA_MOTIVO", desc, km)
                    
                    if (type == "TROCA_DE_TURNO") {
                        scope.launch {
                            val journey = activeJourney ?: return@launch
                            val summary = viewModel.getSummaryForJourney(journey)
                            
                            // 1. Gerar Mensagem WhatsApp
                            val msg = """
                                *RELATÓRIO DE TROCA DE TURNO*
                                🚜 *MÁQUINA:* ${vinculo?.codigoFrotaLocal ?: "---"}
                                👤 *OPERADOR:* ${journey.operatorMatricula}
                                📅 *DATA:* ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}
                                
                                🏁 *KM INICIAL:* ${journey.kmInicial}
                                📈 *KM FINAL:* $km
                                ⚙️ *HORÍMETRO:* ${"%.2f".format(journey.lastHorimetro)}
                                🛣️ *DISTÂNCIA:* ${"%.2f".format(summary.distanceKm)} KM
                                ⛽ *ABASTECIMENTOS:* ${summary.refuelingCount}
                                
                                📝 *OBS:* $observation
                                
                                _Enviado via PLMAGRO Industrial_
                            """.trimIndent()

                            // 2. Abrir WhatsApp
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)

                            // 3. Finalizar Jornada e Sincronizar
                            viewModel.endJourney(km)
                            onShiftChangeComplete()
                        }
                    } else if (type == "ABASTECIMENTO") {
                        onNavigateToRefueling()
                    } else {
                        onBack()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = selectedType != null
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CONFIRMAR PARADA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StopTypeItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NeonGreen else CardBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
