package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.components.NumericKeypad
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.theme.CardBackground
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel

@Composable
fun StartJourneyScreen(viewModel: MainViewModel, onBack: () -> Unit, onStart: (String, String, String) -> Unit) {
    val activeVinculo by viewModel.activeVinculo.collectAsState()
    var step by remember { mutableStateOf(1) } // 1: KM Inicial, 2: Operação, 3: Centro de Custo
    
    // Iniciar KM com o valor que veio do site no momento do vínculo
    var kmInicial by remember { mutableStateOf("") }
    var operacao by remember { mutableStateOf("") }
    var ccusto by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Efeito para carregar o KM do Wialon/PLMView assim que a tela abre
    LaunchedEffect(activeVinculo) {
        activeVinculo?.let { vinculo ->
            if (vinculo.ultimoKmWialon > 0 && kmInicial.isEmpty()) {
                kmInicial = vinculo.ultimoKmWialon.toInt().toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        val currentInput = when(step) {
            1 -> kmInicial
            2 -> operacao
            else -> ccusto
        }
        
        val title = when(step) {
            1 -> "KM Inicial"
            2 -> "Código da Operação"
            else -> "Centro de Custo"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (step > 1) step-- else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                "Início de Jornada",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = NeonGreen,
            trackColor = Color.DarkGray,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NeonGreen,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentInput.ifEmpty { "----" },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        NumericKeypad(
            modifier = Modifier.weight(1f),
            onDigitClick = { digit ->
                when(step) {
                    1 -> if (kmInicial.length < 7) kmInicial += digit
                    2 -> if (operacao.length < 6) operacao += digit
                    3 -> if (ccusto.length < 8) ccusto += digit
                }
            },
            onDeleteClick = {
                when(step) {
                    1 -> if (kmInicial.isNotEmpty()) kmInicial = kmInicial.dropLast(1)
                    2 -> if (operacao.isNotEmpty()) operacao = operacao.dropLast(1)
                    3 -> if (ccusto.isNotEmpty()) ccusto = ccusto.dropLast(1)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlmButton(
            text = if (step < 3) "PRÓXIMO" else "INICIAR JORNADA",
            onClick = {
                if (step < 3) step++ else onStart(kmInicial, operacao, ccusto)
            },
            enabled = currentInput.isNotEmpty()
        )
    }
}
}
