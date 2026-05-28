package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.soniel.plmagro.api.WialonUnit
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.DiagnosticViewModel
import com.soniel.plmagro.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.soniel.plmagro.model.ConnectionStatus

@Composable
fun WialonDiagnosticScreen(
    viewModel: MainViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val eid by viewModel.wialonEid.collectAsState()
    val token by viewModel.wialonToken.collectAsState()
    val units by viewModel.wialonUnits.collectAsState()
    val lastError by viewModel.lastWialonError.collectAsState()
    val diagState by diagnosticViewModel.diagnosticState.collectAsState()
    val recentEvents by viewModel.recentSyncEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val canBusData by viewModel.canBusData.collectAsState()
    
    var showTokenEdit by remember { mutableStateOf(false) }
    var newToken by remember(token) { mutableStateOf(token ?: "") }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (eid == null) {
            viewModel.refreshWialonStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Diagnóstico Sync Industrial", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
            
            IconButton(onClick = { viewModel.performFullSync() }) {
                Icon(Icons.Default.CloudSync, contentDescription = "Full Sync", tint = NeonGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // We use a single LazyColumn to avoid overlap between multiple scrollable areas
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Stats Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESTATÍSTICAS SYNC (HOJE)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiagnosticStatItem("TOTAL", diagState.pendingSync.toString(), if(diagState.pendingSync > 0) Color.Yellow else Color.Gray)
                            DiagnosticStatItem("GPS (IPS)", diagState.pendingTelemetry.toString(), if(diagState.pendingTelemetry > 0) NeonGreen else Color.Gray)
                            DiagnosticStatItem("EVENTOS", diagState.pendingEvents.toString(), if(diagState.pendingEvents > 0) Color.Cyan else Color.Gray)
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ERROS", color = Color.Gray, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(diagState.errorSync.toString(), color = if(diagState.errorSync > 0) Color.Red else Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    if (diagState.errorSync > 0) {
                                        IconButton(
                                            onClick = { viewModel.performFullSync() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiagnosticItem("ÚLTIMA SYNC", if(diagState.lastSyncTime > 0) dateFormat.format(Date(diagState.lastSyncTime)) else "---")
                            DiagnosticItem("LATÊNCIA", "${diagState.apiLatency}ms")
                        }

                        if (diagState.lastSyncError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ERRO ATUAL:", color = Color.Gray, fontSize = 10.sp)
                            Text(diagState.lastSyncError!!, color = Color.Red, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 2. Industrial connections
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CONEXÕES INDUSTRIAIS", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ConnectionChip("GPS", diagState.gps.status)
                            ConnectionChip("WIALON API", diagState.wialon.status)
                            ConnectionChip("IPS TCP", diagState.ips.status)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ConnectionChip("MQTT", diagState.mqtt.status)
                            ConnectionChip("CAN BUS", diagState.can.status)
                            ConnectionChip("CENTRAL WEB", diagState.web.status)
                        }
                    }
                }
            }

            // 3. Hardware moved out of dashboard
            item {
                val storage = DashboardFormatters.formatStorage(diagState.freeDiskMb, diagState.totalDiskMb)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SAÚDE DO HARDWARE (INDUSTRIAL)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiagnosticItem("BATERIA", "${diagState.batteryLevel}% (${if(healthState.isCharging) "CARREGANDO" else "PILHA"})")
                            DiagnosticItem("TEMP BATERIA", "${healthState.batteryTemp}°C")
                            DiagnosticItem("MEMÓRIA", "${healthState.memoryUsageMb} MB")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiagnosticItem("SOCKETS ATIVOS", "${healthState.activeSockets}")
                            DiagnosticItem("ARMAZENAMENTO", storage.percent)
                            DiagnosticItem("SINAL", "${diagState.signalLevel}/4")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiagnosticItem("GPS HW", if (diagState.gpsHardwareActive) "ATIVO" else "INATIVO")
                            DiagnosticItem("DISK", storage.detail)
                            DiagnosticItem("COLETORES", "${healthState.activeCollectors}")
                        }
                    }
                }
            }

            // 3.5 CAN BUS Telemetry
            if (canBusData != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("TELEMETRIA VEICULAR (CAN/OBD2)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DiagnosticItem("ROTAÇÃO (RPM)", "${canBusData?.rpm} rpm")
                                DiagnosticItem("TEMP MOTOR", "${canBusData?.engineTemp}°C")
                                DiagnosticItem("NÍVEL COMBUSTÍVEL", "${canBusData?.fuelLevel}%")
                            }
                        }
                    }
                }
            }

            // 4. Session Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SESSÃO ATIVA", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = if (eid != null) "LOGADO" else "DESCONECTADO",
                            color = if (eid != null) NeonGreen else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        DiagnosticItem("EID", viewModel.maskString(eid))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                DiagnosticItem("TOKEN", viewModel.maskString(token))
                            }
                            TextButton(onClick = { showTokenEdit = !showTokenEdit }) {
                                Text(if (showTokenEdit) "CANCELAR" else "ALTERAR", color = NeonGreen)
                            }
                        }

                        if (showTokenEdit) {
                            PlmTextField(
                                value = newToken,
                                onValueChange = { newToken = it },
                                label = "Novo Token Wialon"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PlmButton(
                                text = "SALVAR TOKEN",
                                onClick = {
                                    viewModel.saveWialonToken(newToken)
                                    showTokenEdit = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Error Banner (if any)
            if (lastError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF442222)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lastError!!, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlmSmallButton(
                        text = if (isLoading) "BUSCANDO..." else "CONEXÃO",
                        onClick = { viewModel.refreshWialonStatus() },
                        icon = if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Sync,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    PlmSmallButton(
                        text = "CERCAS",
                        onClick = { viewModel.syncGeofences() },
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.DarkGray
                    )
                    PlmSmallButton(
                        text = "MOTORISTAS",
                        onClick = { viewModel.syncOperators() },
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1.2f),
                        containerColor = Color.DarkGray
                    )
                }
            }

            // 5. Units List
            item {
                Text("UNIDADES ENCONTRADAS (${units.size})", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            items(units) { unit ->
                UnitItem(unit)
            }

            // 6. Recent Events List
            item {
                Text("ÚLTIMOS EVENTOS SYNC", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            items(recentEvents) { event ->
                OutboxEventItem(event)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OutboxEventItem(event: com.soniel.plmagro.model.OutboxEventEntity) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.tipoEvento, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(event.eventId.take(8), color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    event.syncStatus,
                    color = when(event.syncStatus) {
                        "ENVIADO" -> NeonGreen
                        "ERRO" -> Color.Red
                        "TENTANDO" -> Color.Yellow
                        else -> Color.Gray
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(dateFormat.format(Date(event.timestamp)), color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun PlmSmallButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = NeonGreen,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = if (containerColor == NeonGreen) Color.Black else Color.White,
            disabledContainerColor = Color.DarkGray,
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DiagnosticStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun ConnectionChip(label: String, status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.ONLINE -> NeonGreen
        ConnectionStatus.SYNCING -> Color.Yellow
        ConnectionStatus.ERROR, ConnectionStatus.AUTH_FAILED -> Color.Red
        ConnectionStatus.OFFLINE -> Color.Gray
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(status.name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UnitItem(unit: WialonUnit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(unit.nm, color = Color.White, fontWeight = FontWeight.Bold)
                Text("ID: ${unit.id}", color = Color.Gray, fontSize = 12.sp)
                if (!unit.uid.isNullOrBlank()) {
                    Text("IMEI/UID: ${unit.uid}", color = NeonGreen, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("AVL", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}
