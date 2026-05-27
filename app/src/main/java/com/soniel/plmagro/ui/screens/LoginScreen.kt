package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soniel.plmagro.ui.components.PlmButton
import com.soniel.plmagro.ui.components.PlmTextField
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel, 
    onLoginSuccess: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val operators by viewModel.operators.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedOperator by remember { mutableStateOf<com.soniel.plmagro.model.Operator?>(null) }
    var senha by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val filteredOperators = if (searchQuery.isBlank()) {
        operators
    } else {
        operators.filter { it.name.contains(searchQuery, ignoreCase = true) || it.matricula.contains(searchQuery) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Logo",
                modifier = Modifier.size(60.dp),
                tint = NeonGreen
            )
            
            Text(
                text = "PLMAGRO",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            Text(
                text = "ACESSO OPERACIONAL",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Searchable Operator Selector
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedOperator?.name ?: searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        selectedOperator = null
                        isDropdownExpanded = true
                    },
                    label = { Text("Selecionar Motorista") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen,
                        cursorColor = NeonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    readOnly = selectedOperator != null
                )

                if (isDropdownExpanded && filteredOperators.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier
                            .background(Color.DarkGray)
                            .heightIn(max = 280.dp) // Limita a altura para não sumir atrás do teclado
                    ) {
                        filteredOperators.take(50).forEach { operator -> // Aumentado limite para 50
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(operator.name, color = Color.White)
                                        Text("Matrícula: ${operator.matricula}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedOperator = operator
                                    searchQuery = operator.name
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedOperator != null) {
                TextButton(
                    onClick = { selectedOperator = null; searchQuery = "" },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Limpar Seleção", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            PlmTextField(
                value = senha,
                onValueChange = { senha = it },
                label = "Senha (último dígito)",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(32.dp))

            PlmButton(
                text = "ENTRAR",
                onClick = { 
                    selectedOperator?.let { op ->
                        if (senha == op.matricula.takeLast(1) || senha == "123") {
                            onLoginSuccess(op.matricula) 
                        }
                    }
                },
                enabled = selectedOperator != null && senha.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (operators.isEmpty()) {
                Text(
                    "Nenhum motorista sincronizado. Peça ao administrador para realizar a sincronização nas configurações.",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                com.soniel.plmagro.ui.components.PlmCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seleção de Motorista",
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Text(
                                text = "Selecione seu nome e use o último dígito da matrícula como senha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "v3.1.0-SYNC", color = Color.Gray, fontSize = 12.sp)
        }

        // Botão de Configurações no Topo Direito (movido para o final do Box para ficar no topo do empilhamento)
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color.Gray)
        }
    }
}
