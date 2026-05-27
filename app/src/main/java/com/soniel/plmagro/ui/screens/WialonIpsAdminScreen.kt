package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.core.utils.ClipboardUtils
import com.soniel.plmagro.model.ConnectionStatus
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WialonIpsAdminScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val unitName by viewModel.linkedUnitName.collectAsState()
    val uniqueId by viewModel.linkedUid.collectAsState()
    val host by viewModel.ipsHost.collectAsState()
    val port by viewModel.ipsPort.collectAsState()
    val lastAck by viewModel.lastIpsAck.collectAsState()
    val diagState by viewModel.diagnosticState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeVinculo by viewModel.activeVinculo.collectAsState()

    var editUnitName by remember(unitName) { mutableStateOf(unitName ?: "") }
    var editUniqueId by remember(uniqueId) { mutableStateOf(uniqueId ?: "") }
    var editHost by remember(host) { mutableStateOf(host) }
    var editPort by remember(port) { mutableStateOf(port.toString()) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    "Configuração Wialon IPS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STATUS DA CONEXÃO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusText, statusColor, statusIcon) = when (diagState.ips.status) {
                            ConnectionStatus.ONLINE -> Triple("ONLINE IPS", NeonGreen, Icons.Default.CheckCircle)
                            ConnectionStatus.SYNCING -> Triple("AGUARDANDO ACK", Color.Yellow, Icons.Default.Sync)
                            ConnectionStatus.ERROR -> Triple("OFFLINE IPS", Color.Red, Icons.Default.Error)
                            else -> Triple("OFFLINE IPS", Color.Gray, Icons.Default.Error)
                        }
                        
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusText, color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatusRow("Último Login", if(diagState.ips.lastUpdate > 0) dateFormat.format(Date(diagState.ips.lastUpdate)) else "---")
                    StatusRow("Último ACK", lastAck ?: "Nenhum")
                    StatusRow("Latência", "${diagState.ipsLastLatency}ms")
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENVIADOS", color = Color.Gray, fontSize = 10.sp)
                            Text("${diagState.ipsTotalSent}", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FALHAS", color = Color.Gray, fontSize = 10.sp)
                            Text("${diagState.ipsTotalFailures}", color = if(diagState.ipsTotalFailures > 0) Color.Red else Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ÚLTIMO ENVIO", color = Color.Gray, fontSize = 10.sp)
                            Text(if(diagState.lastIpsSentTime > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(diagState.lastIpsSentTime)) else "---", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    
                    if (diagState.ips.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Erro: ${diagState.ips.errorMessage}", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Status Section
            Text("STATUS DO HARDWARE (HEARTBEAT)", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HardwareStatItem("BATERIA", "${diagState.batteryLevel}%", if(diagState.batteryLevel > 20) NeonGreen else Color.Red)
                        HardwareStatItem("TEMP", "${diagState.batteryTemp}°C", if(diagState.batteryTemp < 45) Color.White else Color.Yellow)
                        HardwareStatItem("DISCO", "${diagState.freeDiskMb}MB", Color.White)
                        HardwareStatItem("GPS HW", if(diagState.gpsHardwareActive) "ATIVO" else "OFF", if(diagState.gpsHardwareActive) NeonGreen else Color.Red)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = if(diagState.isCharging) NeonGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if(diagState.isCharging) "Carregando" else "Na Bateria",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "Sinal: ${if(diagState.ips.status == ConnectionStatus.ONLINE) "Online" else "Desconectado"}",
                            color = if(diagState.ips.status == ConnectionStatus.ONLINE) NeonGreen else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Configuration Section
            Text("PARÂMETROS TÉCNICOS", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            // Link Auto-correction Box
            activeVinculo?.wialonUniqueId?.let { linkUid ->
                if (linkUid != editUniqueId) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Divergência Detectada", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("O ID no vínculo Wialon é: $linkUid", color = Color.LightGray, fontSize = 11.sp)
                            }
                            TextButton(onClick = { editUniqueId = linkUid }) {
                                Text("CORRIGIR", color = NeonGreen)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PlmTextField(
                value = editUnitName,
                onValueChange = { editUnitName = it },
                label = "Nome da Unidade"
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    PlmTextField(
                        value = editUniqueId,
                        onValueChange = { editUniqueId = it },
                        label = "Unique ID / IMEI"
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { ClipboardUtils.copyToClipboard(viewModel.getApplication(), editUniqueId) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = NeonGreen)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    PlmTextField(
                        value = editHost,
                        onValueChange = { editHost = it },
                        label = "Host IPS"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(100.dp)) {
                    PlmTextField(
                        value = editPort,
                        onValueChange = { editPort = it },
                        label = "Porta"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PlmButton(
                text = "SALVAR CONFIGURAÇÃO",
                onClick = {
                    viewModel.saveIpsConfig(editHost, editPort.toIntOrNull() ?: 20332, editUniqueId, editUnitName)
                },
                icon = Icons.Default.Settings
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlmButton(
                text = if (isLoading) "VALIDANDO..." else "VALIDAR CONEXÃO AGORA",
                onClick = { 
                    if (editUniqueId.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Erro: Informe o Unique ID (IMEI) antes de validar.")
                        }
                    } else {
                        viewModel.validateIpsConnection() 
                    }
                },
                icon = Icons.Default.Sync,
                containerColor = Color.DarkGray,
                contentColor = Color.White,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HardwareStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
