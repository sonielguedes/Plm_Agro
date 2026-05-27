package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.NeonGreen

data class Fleet(val id: String, val name: String, val type: String)

@Composable
fun FleetSelectionScreen(
    onFleetSelected: (Fleet) -> Unit,
    fleets: List<Fleet> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredFleets = fleets.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true) ||
            it.type.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SELECAO DE FROTA",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Frotas carregadas do banco local/Wialon",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        PlmTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar frota ou codigo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredFleets.isEmpty()) {
            PlmCard {
                Text(
                    text = "Nenhuma frota local carregada.",
                    color = Color.LightGray,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredFleets) { fleet ->
                PlmCard(
                    modifier = Modifier.clickable { onFleetSelected(fleet) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = fleet.id,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            )
                            Text(fleet.name, color = Color.White)
                            Text(fleet.type, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
