package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.model.OutboxEventEntity
import com.soniel.plmagro.model.OutboxStatus
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OcorrenciasScreen(viewModel: MainViewModel) {
    val pendingCount = viewModel.pendingSyncCount.collectAsStateWithLifecycle().value
    val outboxEvents = viewModel.recentSyncEvents.collectAsStateWithLifecycle().value

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "EVENTOS / OUTBOX",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text("Fila real de sincronizacao", color = Color.Gray)
                }
                Surface(
                    color = if (pendingCount > 0) NeonGreen else Color.DarkGray,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "$pendingCount pendentes",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pendingCount > 0) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (outboxEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum evento pendente no Outbox", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(outboxEvents, key = { it.eventId }) { event ->
                        SyncEventItem(event)
                    }
                }
            }
        }
    }
}

@Composable
fun SyncEventItem(event: OutboxEventEntity) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    val statusColor = when (event.syncStatus) {
        OutboxStatus.ENVIADO -> NeonGreen
        OutboxStatus.ERRO -> Color.Red
        OutboxStatus.TENTANDO -> Color.Yellow
        else -> Color.LightGray
    }
    val icon = when (event.syncStatus) {
        OutboxStatus.ENVIADO -> Icons.Default.CloudDone
        OutboxStatus.ERRO -> Icons.Default.CloudOff
        OutboxStatus.TENTANDO -> Icons.Default.Sync
        else -> Icons.Default.CloudQueue
    }

    PlmCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = statusColor)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.tipoEvento, fontWeight = FontWeight.Bold, color = Color.White)
                Text("ID: ${event.eventId.take(8)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                event.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(event.syncStatus, color = statusColor, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(event.timestamp)), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
