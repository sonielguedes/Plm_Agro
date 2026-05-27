package com.soniel.plmagro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniel.plmagro.ui.components.PlmCard
import com.soniel.plmagro.ui.theme.NeonGreen
import com.soniel.plmagro.viewmodel.MainViewModel

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val activeJourney = viewModel.activeJourney.collectAsStateWithLifecycle().value
    val historicalJourneys = viewModel.historicalJourneys.collectAsStateWithLifecycle().value
    val diagnosticState = viewModel.diagnosticState.collectAsStateWithLifecycle().value
    val lastFinished = historicalJourneys.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            "RELATORIO OPERACIONAL",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
        )
        Text("Dados reais salvos no dispositivo", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ReportCategoryCard("Jornada ativa") {
                    ReportRow("Status", activeJourney?.currentState?.name ?: "SEM_JORNADA")
                    ReportRow("Frota", activeJourney?.vehicleId ?: "---")
                    ReportRow("Operador", activeJourney?.operatorMatricula ?: "---")
                    ReportRow("Produtividade", "${diagnosticState.produtividadePercent}%")
                    ReportRow("Tempo Operando", "${diagnosticState.tempoOperandoMin} min")
                    ReportRow("Tempo Parado", "${diagnosticState.tempoParadoMin} min")
                    ReportRow("KM atual", activeJourney?.lastKm?.toString() ?: "---")
                    ReportRow("KM rodado", activeJourney?.accumulatedDistance?.let { "%.2f km".format(it / 1000.0) } ?: "---")
                    ReportRow("Horimetro", DashboardFormatters.formatHours(activeJourney?.lastHorimetro))
                }
            }
            item {
                ReportCategoryCard("Ultima jornada finalizada") {
                    ReportRow("Frota", lastFinished?.vehicleId ?: "---")
                    ReportRow("Operador", lastFinished?.operatorMatricula ?: "---")
                    ReportRow("KM inicial", lastFinished?.kmInicial?.toString() ?: "---")
                    ReportRow("KM final", lastFinished?.kmFinal?.toString() ?: "---")
                    ReportRow("Horimetro final", DashboardFormatters.formatHours(lastFinished?.lastHorimetro))
                }
            }
            item {
                ReportCategoryCard("Sincronizacao") {
                    ReportRow("Pendentes", diagnosticState.pendingSync.toString())
                    ReportRow("Telemetria", diagnosticState.pendingTelemetry.toString())
                    ReportRow("Eventos", diagnosticState.pendingEvents.toString())
                    ReportRow("Enviados hoje", diagnosticState.sentToday.toString())
                    ReportRow("Erros", diagnosticState.errorSync.toString())
                }
            }
        }
    }
}

@Composable
fun ReportCategoryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    PlmCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 18.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
            content()
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        Text(value, fontWeight = FontWeight.Bold, color = NeonGreen)
    }
}
