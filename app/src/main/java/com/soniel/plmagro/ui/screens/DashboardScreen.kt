package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.model.OperationalState
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel
import com.soniel.plmagro.viewmodel.WialonConnectionStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val DashboardBlack = Color(0xFF121212)
private val DashboardCard = Color(0xFF1E1E1E)
private val DashboardMuted = Color(0xFF9E9E9E)
private val DashboardDanger = Color(0xFFFF5252)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    operatorName: String = "---",
    vehicleId: String = "---",
    vehiclePlate: String = "---",
    wialonUnitName: String? = null,
    kmAtual: String = "0",
    kmRodado: String = "0,00",
    speed: Int = 0,
    gpsLocation: String = "---",
    currentTime: String = "---",
    satelliteCount: Int = 0,
    gpsSignalStatus: String = "Sem sinal",
    onInformOperation: () -> Unit,
    onInformStop: () -> Unit,
    onEndJourney: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLogbook: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToStops: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToIpsAdmin: () -> Unit,
    onNavigateToLinkFleet: () -> Unit,
    onNavigateToOperationalSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val wialonStatus by viewModel.wialonConnectionStatus.collectAsStateWithLifecycle()
    val ipsStatusRaw by viewModel.ipsConnectionStatus.collectAsStateWithLifecycle()
    val activeVinculo by viewModel.activeVinculo.collectAsStateWithLifecycle()
    val activeJourney by viewModel.activeJourney.collectAsStateWithLifecycle()
    val fsmState by viewModel.currentState.collectAsStateWithLifecycle()
    val diagState by viewModel.diagnosticState.collectAsStateWithLifecycle()

    val hasIpsLink = !activeVinculo?.wialonUniqueId.isNullOrBlank()
    val ipsStatus = if (hasIpsLink) ipsStatusRaw else WialonConnectionStatus.OFFLINE
    val statusText = industrialStatusLabel(fsmState, activeJourney != null, speed)

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DashboardBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Simples
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PLMAGRO", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("OPERACIONAL VEICULAR", color = Color.White, fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onNavigateToLogbook) { Icon(Icons.AutoMirrored.Filled.Assignment, "Jornada", tint = NeonGreen) }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Config", tint = Color.White) }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = Color.White) }
                }
            }

            // Status Bar (API/IPS)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusIndicator("API", wialonStatus)
                StatusIndicator("IPS", ipsStatus)
            }

            // Card do Veiculo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DashboardCard),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (activeVinculo == null) "NÃO VINCULADO" else "VINCULADO: ${activeVinculo?.wialonNome}", 
                            color = if (activeVinculo == null) DashboardDanger else NeonGreen, 
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        if (diagState.alertaManutencaoAtivo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("REVISÃO: ${diagState.horasParaManutencao.toInt()}h", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(vehicleId, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Placa: $vehiclePlate", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("OPERADOR: $operatorName", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Grid de Dados 2x2
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimpleStatCard("VELOCIDADE", "$speed km/h", Modifier.weight(1f))
                    SimpleStatCard("KM ATUAL", kmAtual, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimpleStatCard("KM RODADO", "$kmRodado KM", Modifier.weight(1f))
                    SimpleStatCard("SINAL GPS", if(satelliteCount > 0) "Ativo ($satelliteCount)" else "Sem Sinal", Modifier.weight(1f))
                }
            }

            // Status Central
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STATUS INDUSTRIAL", color = DashboardMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(statusText, color = industrialStatusColor(statusText), fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }

            // Botões de Ação Grandes
            Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { if (activeJourney == null) onNavigateToJourney() else onInformOperation() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("OPERAÇÃO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onInformStop,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Pause, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("PARADA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Rodapé
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GPS: $gpsLocation", color = DashboardMuted, fontSize = 10.sp)
                Text(currentTime, color = DashboardMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SimpleStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DashboardCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = DashboardMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StatusIndicator(label: String, status: WialonConnectionStatus) {
    val color = when (status) {
        WialonConnectionStatus.ONLINE -> NeonGreen
        WialonConnectionStatus.SYNCING -> Color.Yellow
        else -> DashboardDanger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(4.dp))
        Text("$label: ${status.name}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun industrialStatusLabel(state: OperationalState, hasJourney: Boolean, speed: Int): String {
    if (!hasJourney) return "SEM JORNADA"
    return when (state) {
        OperationalState.OPERANDO, OperationalState.EM_MOVIMENTO -> "OPERANDO"
        OperationalState.PARADO -> "PARADO"
        OperationalState.PARADA_APONTADA, OperationalState.ABASTECENDO -> "PARADA_APONTADA"
        OperationalState.AGUARDANDO, OperationalState.AGUARDANDO_PARADA, OperationalState.JORNADA_ATIVA -> if (speed > 5) "OPERANDO" else "AGUARDANDO"
        OperationalState.MANUTENCAO -> "MANUTENCAO"
        else -> state.name
    }
}

private fun industrialStatusColor(status: String): Color {
    return when (status) {
        "OPERANDO" -> NeonGreen
        "PARADO" -> Color(0xFFFFC107)
        "PARADA_APONTADA" -> Color(0xFFFFA000)
        "AGUARDANDO" -> Color.White
        "MANUTENCAO" -> DashboardDanger
        else -> Color.White
    }
}
