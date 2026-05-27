package com.soniel.plmagro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniel.plmagro.model.ConnectionStatus
import com.soniel.plmagro.model.DiagnosticRepository
import com.soniel.plmagro.model.DiagnosticState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DiagnosticViewModel(
    private val repository: DiagnosticRepository
) : ViewModel() {

    val diagnosticState: StateFlow<DiagnosticState> = repository.diagnosticState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiagnosticState())

    fun getConnectionColor(status: ConnectionStatus) = when (status) {
        ConnectionStatus.ONLINE -> 0xFF4CAF50 // Green
        ConnectionStatus.SYNCING -> 0xFFFFEB3B // Yellow
        ConnectionStatus.OFFLINE -> 0xFF9E9E9E // Gray
        ConnectionStatus.ERROR -> 0xFFF44336 // Red
        ConnectionStatus.AUTH_FAILED -> 0xFFD32F2F // Darker Red
    }
}
