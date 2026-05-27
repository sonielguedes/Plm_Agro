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
fun RefuelingScreen(onBack: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Litros, 2: KM Atual
    var liters by remember { mutableStateOf("") }
    var kmAtual by remember { mutableStateOf("") }

    val currentInput = if (step == 1) liters else kmAtual
    val title = if (step == 1) "Litros Abastecidos" else "KM Atual"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step == 2) step = 1 else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Abastecimento", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

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
                text = currentInput.ifEmpty { "0" },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        NumericKeypad(
            modifier = Modifier.weight(1f),
            showDecimal = step == 1,
            onDigitClick = { digit ->
                if (step == 1) {
                    if (liters.length < 8) {
                        if (digit == "." && liters.contains(".")) return@NumericKeypad
                        liters += digit
                    }
                } else {
                    if (kmAtual.length < 7) kmAtual += digit
                }
            },
            onDeleteClick = {
                if (step == 1) {
                    if (liters.isNotEmpty()) liters = liters.dropLast(1)
                } else {
                    if (kmAtual.isNotEmpty()) kmAtual = kmAtual.dropLast(1)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlmButton(
            text = if (step == 1) "PRÓXIMO" else "CONFIRMAR",
            onClick = {
                if (step == 1) step = 2 else onConfirm(liters.toDoubleOrNull() ?: 0.0, kmAtual)
            },
            enabled = currentInput.isNotEmpty()
        )
    }
}
