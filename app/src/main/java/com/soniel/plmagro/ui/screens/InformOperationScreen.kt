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

@Composable
fun InformOperationScreen(onBack: () -> Unit, onSave: (String, String) -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Operation Code, 2: Cost Center
    var operationCode by remember { mutableStateOf("") }
    var costCenter by remember { mutableStateOf("") }

    val currentInput = if (step == 1) operationCode else costCenter
    val title = if (step == 1) "Código da Operação" else "Centro de Custo"
    val subTitle = if (step == 1) "Passo 1 de 2" else "Passo 2 de 2"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                "Informar Operação",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "$step. $title",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        )
        Text(subTitle, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // Large Display
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
                    letterSpacing = 8.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Numeric Keypad
        NumericKeypad(
            modifier = Modifier.weight(1f),
            onDigitClick = { digit ->
                if (step == 1) {
                    if (operationCode.length < 6) operationCode += digit
                } else {
                    if (costCenter.length < 8) costCenter += digit
                }
            },
            onDeleteClick = {
                if (step == 1) {
                    if (operationCode.isNotEmpty()) operationCode = operationCode.dropLast(1)
                } else {
                    if (costCenter.isNotEmpty()) costCenter = costCenter.dropLast(1)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlmButton(
            text = if (step == 1) "PRÓXIMO" else "SALVAR",
            onClick = {
                if (step == 1 && operationCode.isNotEmpty()) {
                    step = 2
                } else if (step == 2 && costCenter.isNotEmpty()) {
                    onSave(operationCode, costCenter)
                }
            },
            enabled = currentInput.isNotEmpty()
        )
    }
}
