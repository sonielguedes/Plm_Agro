package com.soniel.plmagro.core.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing captured CAN BUS (J1939/OBD2) parameters.
 */
data class CanBusData(
    val rpm: Int,
    val engineTemp: Float,
    val fuelLevel: Float,
    val fuelConsumption: Float
)

/**
 * Foundation for CAN BUS integration.
 * This manager provides an interface to read from serial or bluetooth OBD2 adapters.
 * Currently it emits simulated data for testing purposes.
 */
@Singleton
class CanBusManager @Inject constructor() {

    private val _rawLogs = MutableStateFlow<List<String>>(emptyList())
    val rawLogs: StateFlow<List<String>> = _rawLogs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private fun addLog(log: String) {
        val time = dateFormat.format(Date())
        val logLine = "[$time] $log"
        val currentList = _rawLogs.value.toMutableList()
        currentList.add(logLine)
        if (currentList.size > 50) {
            currentList.removeAt(0)
        }
        _rawLogs.value = currentList
    }

    // Simulates a connection and continuous data streaming
    val canBusDataFlow: Flow<CanBusData> = flow {
        var baseRpm = 800
        var baseTemp = 85.0f
        var baseFuel = 95.0f
        
        while (true) {
            // Simulate minor fluctuations
            val currentRpm = (baseRpm + (-50..50).random()).coerceIn(0, 4000)
            val currentTemp = (baseTemp + (-2..2).random()).coerceIn(20f, 120f)
            val currentFuel = (baseFuel - 0.01f).coerceAtLeast(0f)
            baseFuel = currentFuel
            
            val consumption = if (currentRpm > 1000) 15.5f else 2.5f
            
            // Simula um pacote hexadecimal sendo recebido do adaptador Bluetooth ELM327
            val hexRpm = String.format("%04X", currentRpm * 4) // OBD2 RPM é (A*256 + B)/4
            val hexTemp = String.format("%02X", (currentTemp + 40).toInt()) // OBD2 Temp é A - 40
            
            val rawPacket = "41 0C ${hexRpm.substring(0, 2)} ${hexRpm.substring(2, 4)} 05 $hexTemp"
            addLog("RX: $rawPacket")

            emit(CanBusData(
                rpm = currentRpm,
                engineTemp = currentTemp,
                fuelLevel = currentFuel,
                fuelConsumption = consumption
            ))
            
            // J1939 / OBD2 update interval (usually frequent, but simulated 2s here)
            delay(2000)
        }
    }
}
