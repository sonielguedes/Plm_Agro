package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.BackgroundDark
import com.soniel.plmagro.ui.theme.DarkGreen
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanBusConfigScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentMode by viewModel.canBusMode.collectAsStateWithLifecycle()
    val currentBtMac by viewModel.canBusBtMac.collectAsStateWithLifecycle()
    val currentUsbPort by viewModel.canBusUsbPort.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf(currentMode) }
    var btMac by remember { mutableStateOf(currentBtMac) }
    var usbPort by remember { mutableStateOf(currentUsbPort) }

    // Atualiza estados caso cheguem do backend (primeira carga)
    LaunchedEffect(currentMode, currentBtMac, currentUsbPort) {
        selectedMode = currentMode
        btMac = currentBtMac
        usbPort = currentUsbPort
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hardware da Máquina (CAN/OBD2)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "TIPO DE CONEXÃO VEICULAR",
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            PlmCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModeOption(
                        title = "Simulador de Testes",
                        subtitle = "Gera dados falsos para testes do Wialon.",
                        icon = Icons.Default.Warning,
                        selected = selectedMode == "SIMULATED",
                        onClick = { selectedMode = "SIMULATED" }
                    )
                    
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                    
                    ModeOption(
                        title = "Bluetooth (ELM327 / OBD2)",
                        subtitle = "Conexão sem fio através de scanner.",
                        icon = Icons.Default.Bluetooth,
                        selected = selectedMode == "BLUETOOTH",
                        onClick = { selectedMode = "BLUETOOTH" }
                    )

                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    ModeOption(
                        title = "Cabo USB (Serial RS232-CAN)",
                        subtitle = "Conexão cabeada para leitura pesada de telemetria.",
                        icon = Icons.Default.Usb,
                        selected = selectedMode == "USB",
                        onClick = { selectedMode = "USB" }
                    )
                }
            }

            if (selectedMode == "BLUETOOTH") {
                Text("PARÂMETROS BLUETOOTH", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                PlmCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Endereço MAC do dispositivo ELM327 pareado:", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlmTextField(
                            value = btMac,
                            onValueChange = { btMac = it },
                            label = "Ex: 00:1D:A5:68:98:8A",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "O tablet precisará estar pareado nas configurações do Android antes de iniciar a leitura.",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (selectedMode == "USB") {
                Text("PARÂMETROS DA PORTA SERIAL (USB)", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                PlmCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nome da porta no sistema Android (TTY):", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlmTextField(
                            value = usbPort,
                            onValueChange = { usbPort = it },
                            label = "Ex: /dev/ttyUSB0",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "O Baud Rate padrão para rede de tratores (J1939) é 250kbps.",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PlmButton(
                text = "SALVAR CONFIGURAÇÃO",
                onClick = {
                    viewModel.saveCanBusConfig(selectedMode, btMac, usbPort)
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) DarkGreen.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = NeonGreen,
                unselectedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = if (selected) NeonGreen else Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = if (selected) NeonGreen else Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
