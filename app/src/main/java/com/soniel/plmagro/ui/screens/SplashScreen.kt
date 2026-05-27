package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onNext()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.LocalShipping,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PLMAGRO",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            )
            Text(
                text = "OPERACIONAL VEICULAR",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            CircularProgressIndicator(color = NeonGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sincronizando dados offline...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(text = "v1.0.0", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
