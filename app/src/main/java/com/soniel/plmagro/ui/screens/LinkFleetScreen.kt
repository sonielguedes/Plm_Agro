package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.api.WialonUnit
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.ConfiguracoesViewModel
import com.soniel.plmagro.viewmodel.LinkFleetViewModel
import com.soniel.plmagro.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LinkFleetScreen(
    viewModel: MainViewModel,
    linkFleetViewModel: LinkFleetViewModel,
    configuracoesViewModel: ConfiguracoesViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val config by viewModel.vehicleConfig.collectAsStateWithLifecycle()
    val wialonUnits by viewModel.wialonUnits.collectAsStateWithLifecycle()
    val isMainLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val lastWialonError by viewModel.lastWialonError.collectAsStateWithLifecycle()
    
    val isLoadingLink by linkFleetViewModel.isLoading.collectAsStateWithLifecycle()
    val isAnyLoading = isMainLoading || isLoadingLink
    val remoteData by linkFleetViewModel.selectedUnitData.collectAsStateWithLifecycle()

    val isAdmin by configuracoesViewModel.desbloqueado.collectAsStateWithLifecycle()
    val isError by configuracoesViewModel.isError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var searchQuery by remember { mutableStateOf("") }
    var manualFleetCode by remember { mutableStateOf("") }
    
    var remotePlaca by remember { mutableStateOf("") }
    var remoteTipo by remember { mutableStateOf("") }

    LaunchedEffect(remoteData) {
        remoteData?.let {
            remotePlaca = it["placa"] as? String ?: ""
            remoteTipo = it["tipo"] as? String ?: ""
        }
    }
    
    var selectedUnit by remember { mutableStateOf<WialonUnit?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }

    val activeVinculo by viewModel.activeVinculo.collectAsStateWithLifecycle()

    LaunchedEffect(isAdmin, showAdminPasswordDialog) {
        if (isAdmin && showAdminPasswordDialog) {
            showAdminPasswordDialog = false
            adminPassword = ""
            selectedUnit?.let { 
                linkFleetViewModel.vincularFrota(
                    codigoFrota = config?.fleetCode ?: manualFleetCode,
                    unit = it,
                    placa = if (remotePlaca.isNotBlank()) remotePlaca else (config?.plate ?: ""),
                    tipo = if (remoteTipo.isNotBlank()) remoteTipo else (config?.type ?: ""),
                    operador = "ADMIN",
                    adminDesbloqueado = true
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        linkFleetViewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    LaunchedEffect(Unit) {
        configuracoesViewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        linkFleetViewModel.vinculoSucesso.collectLatest { success ->
            if (success) {
                kotlinx.coroutines.delay(1000)
                onSuccess()
            }
        }
    }

    val filteredUnits = remember(wialonUnits, searchQuery) {
        if (searchQuery.isEmpty()) wialonUnits
        else wialonUnits.filter { 
            it.nm.contains(searchQuery, ignoreCase = true) || it.id.toString().contains(searchQuery)
        }
    }

    LaunchedEffect(Unit) {
        if (wialonUnits.isEmpty()) {
            viewModel.refreshWialonStatus() // Pre-load units
        }
    }

    LaunchedEffect(Unit) {
        viewModel.vinculoSalvo.collectLatest { success ->
            if (success) onSuccess()
        }
    }

    if (showAdminPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPasswordDialog = false },
            title = { Text("Senha do Administrador", color = Color.White) },
            text = {
                Column {
                    Text("Esta unidade já possui um vínculo. Digite a senha para alterar:", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    PlmTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = "Senha",
                        isPassword = true,
                        modifier = if (isError) Modifier.border(1.dp, Color.Red, RoundedCornerShape(8.dp)) else Modifier
                    )
                    if (isError) {
                        Text("Senha inválida", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        configuracoesViewModel.validarSenha(adminPassword)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("CONFIRMAR", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasswordDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (showConfirmDialog && selectedUnit != null) {
        val fleetToUse = config?.fleetCode ?: manualFleetCode
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Vínculo", color = Color.White) },
            text = { 
                Text(
                    "Confirmar vínculo da frota local $fleetToUse com unidade Wialon ${selectedUnit?.nm}?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        linkFleetViewModel.vincularFrota(
                            codigoFrota = fleetToUse,
                            unit = selectedUnit,
                            placa = if (remotePlaca.isNotBlank()) remotePlaca else (config?.plate ?: ""),
                            tipo = if (remoteTipo.isNotBlank()) remoteTipo else (config?.type ?: ""),
                            operador = "OPERADOR",
                            adminDesbloqueado = isAdmin
                        )
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("CONFIRMAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Vincular Frota Wialon", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
            
            IconButton(onClick = { viewModel.refreshWialonStatus() }, enabled = !isAnyLoading) {
                Icon(
                    if (isAnyLoading) Icons.Default.Refresh else Icons.Default.Sync, 
                    contentDescription = "Atualizar", 
                    tint = if (isAnyLoading) Color.Gray else NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAnyLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = NeonGreen,
                trackColor = Color.DarkGray
            )
        }

        if (lastWialonError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF442222)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(lastWialonError!!, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        PlmTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar Unidade (Nome ou ID)"
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredUnits) { unit ->
                val isCurrentlySelected = selectedUnit?.id == unit.id
                val isAlreadyLinked = config?.wialonUnitId == unit.id
                
                UnitLinkItem(
                    unit = unit,
                    isSelected = isCurrentlySelected,
                    isAlreadyLinked = isAlreadyLinked,
                    onSelect = { 
                        selectedUnit = unit 
                        linkFleetViewModel.fetchUnitData(unit.id)
                        // Sugerir o nome da unidade como código da frota se estiver vazio
                        if (manualFleetCode.isEmpty()) {
                            manualFleetCode = unit.nm
                        }
                    }
                )
            }
        }
        
        if (filteredUnits.isEmpty() && wialonUnits.isNotEmpty()) {
            Text("Nenhuma unidade encontrada para \"$searchQuery\"", color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        }

        if (selectedUnit != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                border = BorderStroke(1.dp, NeonGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RESUMO DO VÍNCULO", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VEÍCULO LOCAL", color = Color.Gray, fontSize = 10.sp)
                            if (config?.fleetCode != null) {
                                Text(config!!.fleetCode, color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                PlmTextField(
                                    value = manualFleetCode,
                                    onValueChange = { manualFleetCode = it },
                                    label = "Digite o código da frota"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("UNIDADE WIALON", color = Color.Gray, fontSize = 10.sp)
                            Text(selectedUnit!!.nm, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (remotePlaca.isNotBlank() || remoteTipo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DADOS DO SERVIDOR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        
                        if (remotePlaca.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Placa:", color = Color.LightGray, fontSize = 12.sp)
                                Text(remotePlaca, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        if (remoteTipo.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tipo:", color = Color.LightGray, fontSize = 12.sp)
                                Text(remoteTipo, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unit ID: ${selectedUnit!!.id}", color = Color.Gray, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    PlmButton(
                        text = if (isLoadingLink) "VINCULANDO..." else "VINCULAR AGORA",
                        onClick = { 
                            if (activeVinculo != null && activeVinculo?.wialonUnitId != selectedUnit?.id && !isAdmin) {
                                showAdminPasswordDialog = true
                            } else {
                                showConfirmDialog = true 
                            }
                        },
                        enabled = !isAnyLoading
                    )
                }
            }
        }
    }
}
}

@Composable
fun UnitLinkItem(unit: WialonUnit, isSelected: Boolean, isAlreadyLinked: Boolean, onSelect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAlreadyLinked) Color(0xFF1B301B) else Color.DarkGray
        ),
        border = if (isSelected) BorderStroke(2.dp, NeonGreen) else null,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(unit.nm, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("ID Wialon: ${unit.id}", color = if (isAlreadyLinked) NeonGreen else Color.Gray, fontSize = 12.sp)
                if (isAlreadyLinked) {
                    Text("ATUALMENTE VINCULADO", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (isSelected || isAlreadyLinked) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
            }
        }
    }
}
