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
    onNavigateToMessages: () -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val wialonStatus by viewModel.wialonConnectionStatus.collectAsStateWithLifecycle()
    val ipsStatusRaw by viewModel.ipsConnectionStatus.collectAsStateWithLifecycle()
    val activeVinculo by viewModel.activeVinculo.collectAsStateWithLifecycle()
    val activeJourney by viewModel.activeJourney.collectAsStateWithLifecycle()
    val fsmState by viewModel.currentState.collectAsStateWithLifecycle()
    val diagState by viewModel.diagnosticState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()

    val hasIpsLink = !activeVinculo?.wialonUniqueId.isNullOrBlank()
    val ipsStatus = if (hasIpsLink) ipsStatusRaw else WialonConnectionStatus.OFFLINE
    val statusText = industrialStatusLabel(fsmState, activeJourney != null, speed)

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DashboardCard,
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PLMAGRO", color = NeonGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("MENU INDUSTRIAL", color = Color.Gray, fontSize = 12.sp)
                }
                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mensagens da Central", color = Color.White, modifier = Modifier.weight(1f))
                            if (unreadCount > 0) {
                                Badge(containerColor = Color.Red) {
                                    Text("$unreadCount", color = Color.White)
                                }
                            }
                        }
                    },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToMessages() 
                    },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = Color.Red)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Message, null, tint = NeonGreen)
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Diário de Bordo", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToLogbook() 
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Histórico de Viagens", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToHistory() 
                    },
                    icon = { Icon(Icons.Default.History, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Diagnóstico de Hardware", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToDiagnostic() 
                    },
                    icon = { Icon(Icons.Default.Analytics, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Fila de Sincronização", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToEvents() 
                    },
                    icon = { Icon(Icons.Default.SyncAlt, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Sincronizar Agora", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onSyncNow()
                        }
                    },
                    icon = { Icon(Icons.Default.Refresh, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Vincular Frota (Wialon)", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToLinkFleet() 
                    },
                    icon = { Icon(Icons.Default.Link, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Configurações", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToSettings() 
                    },
                    icon = { Icon(Icons.Default.Settings, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Sobre o Sistema", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToAbout() 
                    },
                    icon = { Icon(Icons.Default.Info, null, tint = NeonGreen) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Color.DarkGray)
                
                NavigationDrawerItem(
                    label = { Text("Sair do Aplicativo", color = DashboardDanger) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onLogout() 
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = DashboardDanger) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
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
                // Header com Título à esquerda e Menu à direita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("PLMAGRO", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("OPERACIONAL VEICULAR", color = Color.White, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateToMessages) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = Color.Red) {
                                            Text("$unreadCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Email, "Mensagens", tint = NeonGreen)
                            }
                        }
                        IconButton(onClick = onNavigateToLogbook) { 
                            Icon(Icons.AutoMirrored.Filled.Assignment, "Jornada", tint = NeonGreen) 
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Abrir Menu", tint = Color.White)
                        }
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

                // Grid de Dados 3x2 (Fase 4: Produtividade)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SimpleStatCard("VELOCIDADE", "$speed km/h", Modifier.weight(1f))
                        SimpleStatCard("KM ATUAL", kmAtual, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SimpleStatCard("PRODUTIVIDADE", "${diagState.produtividadePercent}%", Modifier.weight(1f), 
                            valueColor = if(diagState.produtividadePercent > 80) NeonGreen else if(diagState.produtividadePercent > 50) Color.Yellow else DashboardDanger)
                        SimpleStatCard("VELOC. MÉDIA", "${"%.1f".format(diagState.velocidadeMediaOperacao)} km/h", Modifier.weight(1f))
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
}

@Composable
private fun SimpleStatCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DashboardCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = DashboardMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
