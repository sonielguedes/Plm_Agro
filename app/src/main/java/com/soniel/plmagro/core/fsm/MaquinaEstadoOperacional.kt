package com.soniel.plmagro.core.fsm

import android.util.Log
import com.soniel.plmagro.core.eventbus.OperationalEventBus
import com.soniel.plmagro.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MaquinaEstadoOperacional(
    private val repository: PlmRepository,
    initialState: OperationalState = OperationalState.SEM_JORNADA
) {
    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<OperationalState> = _currentState.asStateFlow()

    private var stopDetectionStartTime: Long = 0

    suspend fun processEvent(event: OperationalEvent, currentJourney: Journey?) {
        val nextState = when (event) {
            is OperationalEvent.JourneyStarted, is OperationalEvent.JornadaIniciada -> {
                OperationalState.OPERANDO
            }
            is OperationalEvent.SpeedChanged -> {
                handleSpeedChange(event.speedKmh, _currentState.value)
            }
            is OperationalEvent.MotivoParadaInformado -> {
                OperationalState.PARADA_APONTADA
            }
            is OperationalEvent.OperationStarted, is OperationalEvent.OperacaoAlterada -> {
                OperationalState.OPERANDO
            }
            is OperationalEvent.AbastecimentoRegistrado -> {
                OperationalState.ABASTECENDO
            }
            is OperationalEvent.JourneyFinished, is OperationalEvent.JornadaFinalizada -> {
                OperationalState.FINALIZADA
            }
            is OperationalEvent.GpsUpdated -> {
                if (_currentState.value == OperationalState.ERRO_GPS) {
                    OperationalState.OPERANDO
                } else _currentState.value
            }
            is OperationalEvent.GpsLost, is OperationalEvent.GpsError -> OperationalState.ERRO_GPS
            is OperationalEvent.WialonDisconnected -> OperationalState.OFFLINE_OPERACIONAL
            is OperationalEvent.WialonConnected -> {
                if (_currentState.value == OperationalState.OFFLINE_OPERACIONAL) {
                    OperationalState.OPERANDO
                } else _currentState.value
            }
            else -> _currentState.value
        }

        if (nextState != _currentState.value) {
            transitionTo(nextState, event)
        }
    }

    private fun handleSpeedChange(speedKmh: Float, current: OperationalState): OperationalState {
        return when {
            speedKmh > 5f -> {
                if (current != OperationalState.OPERANDO) {
                    OperationalEventBus.tryEmit(OperationalEvent.MovimentoDetectado)
                }
                stopDetectionStartTime = 0
                OperationalState.OPERANDO
            }
            speedKmh <= 1f && current == OperationalState.OPERANDO -> {
                if (stopDetectionStartTime == 0L) {
                    stopDetectionStartTime = System.currentTimeMillis()
                    OperationalState.PARADO
                } else if (System.currentTimeMillis() - stopDetectionStartTime > 300_000) { // 5 min para auto-parada industrial
                    val duration = (System.currentTimeMillis() - stopDetectionStartTime) / 1000
                    OperationalEventBus.tryEmit(OperationalEvent.ParadaDetected(duration))
                    OperationalState.AGUARDANDO_PARADA
                } else {
                    OperationalState.PARADO
                }
            }
            else -> current
        }
    }

    private suspend fun transitionTo(newState: OperationalState, cause: OperationalEvent? = null) {
        val oldState = _currentState.value
        Log.i("FSM", "FSM_STATE_CHANGED: $oldState -> $newState | Causa: ${cause?.javaClass?.simpleName}")
        _currentState.value = newState
        
        val description = when(newState) {
            OperationalState.OPERANDO -> "Operação Iniciada/Retomada"
            OperationalState.PARADO -> "Máquina Parada (Detectado)"
            OperationalState.PARADA_APONTADA -> (cause as? OperationalEvent.MotivoParadaInformado)?.reason ?: "Parada Apontada"
            else -> null
        }
        
        repository.updateJourneyState(newState, description)
    }
    
    fun forceState(state: OperationalState) {
        _currentState.value = state
    }
}
