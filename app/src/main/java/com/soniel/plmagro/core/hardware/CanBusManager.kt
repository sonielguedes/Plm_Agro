package com.soniel.plmagro.core.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

            emit(
                CanBusData(
                    rpm = currentRpm,
                    engineTemp = currentTemp,
                    fuelLevel = currentFuel,
                    fuelConsumption = consumption
                )
            )
            
            // J1939 / OBD2 update interval (usually frequent, but simulated 2s here)
            delay(2000)
        }
    }
}
