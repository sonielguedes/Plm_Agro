package com.soniel.plmagro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.core.utils.ClipboardUtils
import com.soniel.plmagro.model.ConnectionStatus
import com.soniel.plmagro.model.TechnicalStatus
import com.soniel.plmagro.viewmodel.ConfiguracoesViewModel
import com.soniel.plmagro.viewmodel.DiagnosticViewModel
import com.soniel.plmagro.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    configuracoesViewModel: ConfiguracoesViewModel,
    onBack: () -> Unit,
    onNavigateToDiagnostic: (() -> Unit)? = null,
    onNavigateToLinkFleet: (() -> Unit)? = null,
    onNavigateToWialonIpsAdmin: (() -> Unit)? = null,
    onNavigateToCanBusConfig: (() -> Unit)? = null
) {
    val config by viewModel.vehicleConfig.collectAsStateWithLifecycle()
    val diagnosticState by diagnosticViewModel.diagnosticState.collectAsStateWithLifecycle()
    val isAdmin by configuracoesViewModel.desbloqueado.collectAsStateWithLifecycle()
    val isError by configuracoesViewModel.isError.collectAsStateWithLifecycle()
    val activeJourney by viewModel.activeJourney.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val health by viewModel.systemHealth.collectAsStateWithLifecycle()
    val activeVinculo by viewModel.activeVinculo.collectAsStateWithLifecycle()
    val wialonStatus by viewModel.wialonConnectionStatus.collectAsStateWithLifecycle()
    val savedIpsHost by viewModel.ipsHost.collectAsStateWithLifecycle()
    val savedIpsPort by viewModel.ipsPort.collectAsStateWithLifecycle()
    val linkedUid by viewModel.linkedUid.collectAsStateWithLifecycle()
    val linkedUnitName by viewModel.linkedUnitName.collectAsStateWithLifecycle()
    val autoStopTimeout by viewModel.autoStopTimeoutMinutes.collectAsStateWithLifecycle()
    val satelliteMode by viewModel.satelliteMode.collectAsStateWithLifecycle()
    val supervisorMode by viewModel.supervisorMode.collectAsStateWithLifecycle()
    val savedErpApiUrl by viewModel.erpApiUrl.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        configuracoesViewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var fleetCode by remember(config) { mutableStateOf(config?.fleetCode ?: "") }
    var plate by remember(config) { mutableStateOf(config?.plate ?: "") }
    var vehicleType by remember(config) { mutableStateOf(config?.type ?: "") }
    
    var ipsHost by remember(savedIpsHost) { mutableStateOf(savedIpsHost) }
    var ipsPort by remember(savedIpsPort) { mutableStateOf(savedIpsPort.toString()) }
    var autoStopMinutes by remember(autoStopTimeout) { mutableStateOf(autoStopTimeout.toString()) }
    
    var maintenanceTarget by remember(config) { mutableStateOf(config?.horimetroManutencao?.toString() ?: "0.0") }
    var maintenanceAlertThreshold by remember(config) { mutableStateOf(config?.alertaManutencaoHoras?.toString() ?: "50.0") }
    var erpApiUrl by remember { mutableStateOf(savedErpApiUrl) }

    LaunchedEffect(config, wialonStatus, savedIpsHost, savedIpsPort, linkedUid, linkedUnitName, autoStopTimeout, savedErpApiUrl) {
        fleetCode = config?.fleetCode ?: ""
        plate = config?.plate ?: ""
        vehicleType = config?.type ?: ""
        ipsHost = savedIpsHost
        ipsPort = savedIpsPort.toString()
        autoStopMinutes = autoStopTimeout.toString()
        maintenanceTarget = config?.horimetroManutencao?.toString() ?: "0.0"
        maintenanceAlertThreshold = config?.alertaManutencaoHoras?.toString() ?: "50.0"
        erpApiUrl = savedErpApiUrl
    }
    
    var adminPassword by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        topBar = {
            Column(modifier = Modifier.background(DarkGreen).padding(top = 32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonGreen)
                    }
                    Text(
                        "CONFIGURAÇÕES OPERACIONAIS", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusIndicator(label = "GPS", techStatus = diagnosticState.gps)
                    StatusIndicator(label = "MQTT", techStatus = diagnosticState.mqtt)
                    StatusIndicator(label = "WIALON", techStatus = diagnosticState.wialon)
                    StatusIndicator(label = "CAN", techStatus = diagnosticState.can)
                    StatusIndicator(label = "WEB", techStatus = diagnosticState.web)
                }

                // Error Banner for Wialon
                if (diagnosticState.wialon.status == ConnectionStatus.ERROR || diagnosticState.wialon.status == ConnectionStatus.AUTH_FAILED) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF442222)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "WIALON: ${diagnosticState.wialon.errorMessage ?: "Erro de autenticação ou conexão"}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Info Summary Card (Enterprise Industrial)
            PlmCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("RESUMO DO SISTEMA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ID Único do Tablet:", color = Color.LightGray, fontSize = 10.sp)
                            Text(viewModel.deviceUniqueId, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(
                            onClick = { 
                                com.soniel.plmagro.core.utils.ClipboardUtils.copyToClipboard(viewModel.getApplication(), viewModel.deviceUniqueId) 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar ID", tint = NeonGreen, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InfoRow(label = "Jornada Ativa:", value = activeJourney?.operatorMatricula ?: "NENHUMA", color = if (activeJourney != null) NeonGreen else Color.Gray)
                    val opCount by viewModel.operators.collectAsStateWithLifecycle()
                    InfoRow(label = "Motoristas na Memória:", value = "${opCount.size}", color = if (opCount.isNotEmpty()) NeonGreen else Color.Yellow)
                    
                    val totalPending = diagnosticState.pendingSync + diagnosticState.pendingTelemetry + diagnosticState.pendingEvents
                    InfoRow(label = "Eventos Pendentes:", value = "$totalPending", color = if (totalPending > 0) Color.Yellow else NeonGreen)
                    
                    if (diagnosticState.lastSyncError != null) {
                        InfoRow(label = "Último Erro Sync:", value = diagnosticState.lastSyncError ?: "", color = Color.Red)
                    }

                    InfoRow(label = "Latência MQTT:", value = "${health.mqttLatency}ms")
                    InfoRow(label = "Última Sinc.:", value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(health.lastSync)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security Section
            Text("SEGURANÇA E AUDITORIA", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            PlmCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isAdmin) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlmTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                label = "Senha Administrativa",
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isError) 2.dp else 0.dp,
                                        color = if (isError) Color.Red else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                isPassword = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    configuracoesViewModel.validarSenha(adminPassword)
                                    adminPassword = "" 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ACESSO ADMINISTRATIVO ATIVO", color = NeonGreen, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { configuracoesViewModel.logout() }) {
                                Text("BLOQUEAR", color = Color.Red)
                            }
                        }
                        Text("As alterações serão auditadas e vinculadas ao operador atual.", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configuration Sections (Restricted)
            AnimatedVisibility(visible = isAdmin) {
                Column {
                    // Atalho Rápido de Sincronização Industrial
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("SINCRONIZAÇÃO RÁPIDA", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlmButton(
                                    text = "Motoristas",
                                    onClick = { viewModel.syncOperators() },
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color.DarkGray,
                                    contentColor = Color.White
                                )
                                PlmButton(
                                    text = "Cercas",
                                    onClick = { viewModel.syncGeofences() },
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color.DarkGray,
                                    contentColor = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("VÍNCULO DE HARDWARE")
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PlmTextField(value = fleetCode, onValueChange = { fleetCode = it }, label = "Código da Frota")
                            Spacer(Modifier.height(8.dp))
                            PlmTextField(value = plate, onValueChange = { plate = it }, label = "Placa")
                            Spacer(Modifier.height(8.dp))
                            PlmTextField(value = vehicleType, onValueChange = { vehicleType = it }, label = "Tipo de Equipamento")
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("UNIDADE WIALON", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(activeVinculo?.wialonNome ?: "NÃO VINCULADO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (activeVinculo != null) {
                                        Text("ID: ${activeVinculo?.wialonUnitId}", color = NeonGreen, fontSize = 12.sp)
                                        Text("STATUS: ${wialonStatus.name}", color = if(wialonStatus.name == "ONLINE") NeonGreen else Color.Gray, fontSize = 10.sp)
                                    }
                                }
                                Button(
                                    onClick = { onNavigateToLinkFleet?.invoke() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeVinculo != null) Color(0xFF2E7D32) else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if (activeVinculo != null) "ALTERAR VÍNCULO" else "VINCULAR",
                                        color = if (activeVinculo != null) Color.White else NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("GATEWAY WIALON IPS")
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PlmTextField(value = ipsHost, onValueChange = { ipsHost = it }, label = "Host IPS (ex: 193.193.165.165)")
                            Spacer(Modifier.height(8.dp))
                            PlmTextField(value = ipsPort, onValueChange = { ipsPort = it }, label = "Porta IPS (ex: 20332)")
                            
                            Spacer(Modifier.height(12.dp))
                            
                            PlmButton(
                                text = "CONFIGURAÇÃO AVANÇADA IPS",
                                onClick = { onNavigateToWialonIpsAdmin?.invoke() },
                                icon = Icons.Default.Settings,
                                containerColor = Color.DarkGray,
                                contentColor = NeonGreen
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            PlmButton(
                                text = "CONFIGURAÇÃO DA REDE CAN (OBD2)",
                                onClick = { onNavigateToCanBusConfig?.invoke() },
                                icon = Icons.Default.Bluetooth,
                                containerColor = Color.DarkGray,
                                contentColor = NeonGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("INTEGRAÇÃO ERP CORPORATIVO")
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PlmTextField(
                                value = erpApiUrl,
                                onValueChange = { erpApiUrl = it },
                                label = "URL da API do ERP (ex: https://api.empresa.com.br)",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("INTELIGÊNCIA OPERACIONAL")
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PlmTextField(
                                value = autoStopMinutes,
                                onValueChange = { autoStopMinutes = it },
                                label = "Alerta de Parada (minutos)",
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Text("Tempo de inatividade (0 km/h) para pedir o motivo da parada.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.DarkGray)
                            Spacer(Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("MODO SATELITAL OTIMIZADO", color = if(satelliteMode) NeonGreen else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Reduz o tráfego de dados para antenas M2M/Satélite.", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = satelliteMode,
                                    onCheckedChange = { viewModel.setSatelliteMode(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonGreen,
                                        checkedTrackColor = DarkGreen,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("PREDIÇÃO DE MANUTENÇÃO")
                    PlmCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlmTextField(
                                    value = maintenanceTarget,
                                    onValueChange = { maintenanceTarget = it },
                                    label = "Próxima Manut. (Horas)",
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                PlmTextField(
                                    value = maintenanceAlertThreshold,
                                    onValueChange = { maintenanceAlertThreshold = it },
                                    label = "Alerta Antecipado (h)",
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                            Text("Horímetro atual: ${"%.1f".format(activeJourney?.lastHorimetro ?: 0.0)}h", fontSize = 11.sp, color = NeonGreen, modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader("DIAGNÓSTICO AVANÇADO")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticSmallButton(
                            modifier = Modifier.weight(1f),
                            label = "WIALON",
                            icon = Icons.Default.Cloud,
                            onClick = { onNavigateToDiagnostic?.invoke() }
                        )
                        DiagnosticSmallButton(
                            modifier = Modifier.weight(1f),
                            label = "LIMPAR CACHE",
                            icon = Icons.Default.DeleteSweep,
                            onClick = { /* Audit log implementation */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PlmButton(
                        text = "APLICAR ALTERAÇÕES",
                        onClick = {
                            viewModel.saveVehicleConfig(fleetCode, plate, vehicleType)
                            viewModel.saveIpsConfig(
                                host = ipsHost,
                                port = ipsPort.toIntOrNull() ?: 20332,
                                uniqueId = linkedUid ?: "",
                                unitName = linkedUnitName ?: ""
                            )
                            viewModel.saveAutoStopTimeout(autoStopMinutes.toIntOrNull() ?: 5)
                            viewModel.saveMaintenanceConfig(
                                target = maintenanceTarget.toDoubleOrNull() ?: 0.0,
                                alertAt = maintenanceAlertThreshold.toDoubleOrNull() ?: 50.0
                            )
                            viewModel.setErpApiUrl(erpApiUrl)
                            
                            onBack()
                        },
                        enabled = fleetCode.isNotEmpty() && plate.isNotEmpty()
                    )
                }
            }

            if (!isAdmin) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                        Text("Configurações Bloqueadas", color = Color.Gray)
                        Text("Insira a senha para habilitar alterações", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusIndicator(label: String, techStatus: TechnicalStatus) {
    val color = when(techStatus.status) {
        ConnectionStatus.ONLINE -> NeonGreen
        ConnectionStatus.SYNCING -> Color.Yellow
        ConnectionStatus.OFFLINE -> Color.Gray
        ConnectionStatus.ERROR -> Color.Red
        ConnectionStatus.AUTH_FAILED -> Color.Red
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title, 
        color = NeonGreen, 
        fontSize = 12.sp, 
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun DiagnosticSmallButton(modifier: Modifier = Modifier, label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
