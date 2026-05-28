package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.theme.*
import com.soniel.plmagro.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanBusDiagnosticScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val rawLogs by viewModel.canBusRawLogs.collectAsState()
    val canData by viewModel.canBusData.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll para o último log
    LaunchedEffect(rawLogs.size) {
        if (rawLogs.isNotEmpty()) {
            listState.animateScrollToItem(rawLogs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor de Rede CAN / OBD2", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackground)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Status da Interface",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (rawLogs.isNotEmpty()) "Conectado / Recebendo Dados" else "Aguardando Tráfego...",
                        color = if (rawLogs.isNotEmpty()) NeonGreen else StatusStopped,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (rawLogs.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = StatusStopped,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Real-time Decoded Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DiagnosticCard(
                    title = "RPM",
                    value = "${canData?.rpm ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticCard(
                    title = "Temp Motor",
                    value = "${canData?.engineTemp ?: 0f} °C",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticCard(
                    title = "Nível Combust.",
                    value = "${canData?.fuelLevel?.toInt() ?: 0}%",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Terminal (Raw Hex Payload)",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Raw Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black) // Classic terminal background
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rawLogs) { log ->
                        Text(
                            text = log,
                            color = Color(0xFF00FF00), // Classic terminal green
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
