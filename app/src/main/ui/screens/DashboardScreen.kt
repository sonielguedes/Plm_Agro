package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.model.OperationalState
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.*
import com.soniel.plmagro.viewmodel.MainViewModel
import com.soniel.plmagro.viewmodel.WialonConnectionStatus
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    vehicleId: String = "---",
    vehiclePlate: String = "---",
    wialonUnitName: String? = null,
    operatorName: String = "---",
    speed: Int = 0,
    onInformOperation: () -> Unit,
    onInformStop: () -> Unit,
    onEndJourney: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val wialonStatus by viewModel.wialonConnectionStatus.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val activeJourney by viewModel.activeJourney.collectAsState()
    val geofence by viewModel.currentGeofence.collectAsState()
    val geofenceName = geofence?.name
    val isSpeeding by viewModel.isSpeedingInGeofence.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when(wialonStatus) {
                            WialonConnectionStatus.ONLINE -> NeonGreen
                            WialonConnectionStatus.SYNCING -> Color.Yellow
                            else -> Color.Red
                        }
                        val statusText = when(wialonStatus) {
                            WialonConnectionStatus.ONLINE -> "ONLINE"
                            WialonConnectionStatus.SYNCING -> "SYNC"
                            else -> "OFFLINE"
                        }
                        
                        Surface(
                            color = statusColor,
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "WIALON: $statusText | OUTBOX: $pendingSync",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "PLMAGRO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (geofenceName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ÁREA: $geofenceName",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                            if ((geofence?.maxSpeed ?: 0f) > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "LIMIT: ${geofence?.maxSpeed?.toInt()} KM/H",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSpeeding) Color.Red else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Row {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Histórico", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color.White)
                    }
                    IconButton(onClick = onEndJourney) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vehicle Card
            PlmCard {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NeonGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (wialonUnitName != null) "VINCULADO: $wialonUnitName" else "NÃO VINCULADO",
                            color = if (wialonUnitName != null) NeonGreen else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(vehicleId, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NeonGreen)
                        Text("Placa: $vehiclePlate", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("OPERADOR", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(operatorName, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry Main
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    label = "VELOCIDADE",
                    value = "$speed km/h",
                    icon = Icons.Default.Speed,
                    iconColor = if (isSpeeding) Color.Red else if (speed > 5) StatusMovement else Color.Gray,
                    isCritical = isSpeeding
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    label = "KM ATUAL",
                    value = activeJourney?.lastKm?.toString() ?: "---",
                    icon = Icons.Default.AddLocation
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    label = "KM JORNADA",
                    value = activeJourney?.let { (it.lastKm - it.kmInicial).toString() } ?: "0",
                    icon = Icons.Default.Route,
                    iconColor = NeonGreen
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    label = "GPS",
                    value = if (activeJourney?.lastLat != null) "SINAL OK" else "BUSCANDO",
                    subValue = activeJourney?.let { "${"%.4f".format(it.lastLat)}, ${"%.4f".format(it.lastLng)}" } ?: "---",
                    icon = Icons.Default.CellTower
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Card
            PlmCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ESTADO OPERACIONAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    val statusColor = when (currentState) {
                        OperationalState.EM_MOVIMENTO -> StatusMovement
                        OperationalState.PARADO_AGUARDANDO_MOTIVO -> StatusStopped
                        OperationalState.PARADA_APONTADA -> NeonGreen
                        else -> Color.Gray
                    }
                    Text(
                        currentState.name.replace("_", " "),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlmButton(
                    text = "Operação",
                    onClick = onInformOperation,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Settings
                )
                PlmButton(
                    text = "Parada",
                    onClick = onInformStop,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.DarkGray,
                    contentColor = Color.White,
                    icon = Icons.Default.Pause
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("V: 3.0.0-CORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(System.currentTimeMillis().toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    iconColor: Color = Color.Gray,
    isCritical: Boolean = false
) {
    val backgroundColor = if (isCritical) Color(0xFF442222) else CardBackground
    
    PlmCard(modifier = modifier, containerColor = backgroundColor) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            if (subValue != null) {
                Text(subValue, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
        }
    }
}
