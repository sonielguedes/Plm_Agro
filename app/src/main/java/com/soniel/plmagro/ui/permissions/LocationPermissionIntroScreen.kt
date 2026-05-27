package com.soniel.plmagro.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.theme.NeonGreen

@Composable
fun LocationPermissionIntroScreen(
    onActivate: () -> Unit,
    onContinueWithoutGps: () -> Unit,
    onOpenGpsSettings: () -> Unit,
    permissionsDenied: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LOCALIZAÇÃO OPERACIONAL",
                color = NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Animation Placeholder
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "O PLMAGRO utiliza GPS realtime para:",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                BulletPoint("KM rodado")
                BulletPoint("telemetria operacional")
                BulletPoint("rastreabilidade")
                BulletPoint("operação offline")
                BulletPoint("auditoria operacional")
            }

            Spacer(modifier = Modifier.weight(1f))

            if (permissionsDenied) {
                Text(
                    "As permissões de localização foram negadas. O app funcionará em modo limitado.",
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onActivate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "ATIVAR TELEMETRIA",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenGpsSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Text("CONFIGURAR GPS (SE DESLIGADO)", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onContinueWithoutGps,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CONTINUAR EM MODO OFFLINE / SEM GPS", color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Segurança Operacional Industrial", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(NeonGreen, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(12.dp))
        Text(text, color = Color.LightGray, fontSize = 16.sp)
    }
}
