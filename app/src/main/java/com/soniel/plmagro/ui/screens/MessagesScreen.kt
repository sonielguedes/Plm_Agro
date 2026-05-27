package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.SpeakerNotesOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.model.MessageEntity
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MENSAGENS DA CENTRAL",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (unreadCount > 0) {
                        Text(
                            "$unreadCount mensagens não lidas",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllMessagesAsRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("LIMPAR", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SpeakerNotesOff, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhuma mensagem recebida", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(messages.sortedByDescending { it.timestamp }, key = { it.id }) { message ->
                        MessageItem(message) {
                            viewModel.markMessageAsRead(message.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: MessageEntity, onMarkRead: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val isUnread = !message.isRead
    val priorityColor = when (message.priority) {
        2 -> Color.Red
        1 -> Color.Yellow
        else -> NeonGreen
    }

    PlmCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isUnread) Icons.Default.Email else Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = if (isUnread) priorityColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (message.priority >= 1) "ALERTA CRÍTICO" else "MENSAGEM",
                        color = if (isUnread) priorityColor else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    dateFormat.format(Date(message.timestamp)),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = message.text,
                color = if (isUnread) Color.White else Color.LightGray,
                fontSize = 16.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
            )
            
            if (isUnread) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onMarkRead,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("MARCAR COMO LIDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                    }
                }
            }
        }
    }
}
