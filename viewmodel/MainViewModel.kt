package com.soniel.plmagro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniel.plmagro.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PlmRepository) : ViewModel() {
    val vehicleConfig = repository.vehicleConfig.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    
    val activeJourney = repository.activeJourney.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed

    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving

    fun saveVehicleConfig(fleet: String, plate: String, type: String) {
        viewModelScope.launch {
            repository.saveVehicleConfig(VehicleConfig(fleetCode = fleet, plate = plate, type = type))
        }
    }

    fun loginAndStartJourney(matricula: String, km: Int, opCode: String, cc: String) {
        viewModelScope.launch {
            val operator = repository.getOperator(matricula) ?: Operator(matricula, "Operador $matricula")
            repository.saveOperator(operator)
            
            val config = vehicleConfig.value
            repository.startJourney(
                Journey(
                    operatorMatricula = matricula,
                    vehicleId = config?.fleetCode ?: "DESCONHECIDO",
                    kmInicial = km,
                    operationCode = opCode,
                    costCenter = cc
                )
            )
        }
    }

    fun endJourney(kmFinal: Int) {
        viewModelScope.launch {
            val journey = activeJourney.value ?: return@launch
            repository.updateJourney(
                journey.copy(
                    kmFinal = kmFinal,
                    endTime = System.currentTimeMillis(),
                    isFinished = true
                )
            )
        }
    }

    fun updateSpeed(newSpeed: Int) {
        _speed.value = newSpeed
        _isMoving.value = newSpeed > 5
    }
}
