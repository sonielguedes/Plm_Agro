package com.soniel.plmagro.model

sealed class OperationalEvent {
    data class GpsUpdated(
        val lat: Double, 
        val lng: Double, 
        val alt: Double, 
        val accuracy: Float, 
        val heading: Float, 
        val speed: Float,
        val satellites: Int = 0
    ) : OperationalEvent()
    
    data class SpeedChanged(val speedKmh: Float) : OperationalEvent()
    
    object MqttConnected : OperationalEvent()
    object MqttDisconnected : OperationalEvent()
    object WialonConnected : OperationalEvent()
    object WialonDisconnected : OperationalEvent()
    
    data class ParadaDetected(val durationSeconds: Long) : OperationalEvent()
    object MovimentoDetectado : OperationalEvent()
    
    data class OperationStarted(val operation: String) : OperationalEvent()
    data class JourneyStarted(val journey: Journey) : OperationalEvent()
    data class JourneyFinished(val journey: Journey) : OperationalEvent()
    
    object GpsLost : OperationalEvent()
    object GpsRecovered : OperationalEvent()
    
    data class OutboxPending(val count: Int) : OperationalEvent()
    data class OutboxSent(val eventId: String) : OperationalEvent()
    
    // Legacy support or internal
    data class JornadaIniciada(val journey: Journey) : OperationalEvent()
    data class JornadaFinalizada(val journey: Journey) : OperationalEvent()
    data class ParadaDetectada(val durationSeconds: Long) : OperationalEvent()
    data class JornadaAtualizada(val journey: Journey) : OperationalEvent()
    data class MotivoParadaInformado(val reason: String) : OperationalEvent()
    data class AbastecimentoRegistrado(val liters: Float, val currentKm: Int) : OperationalEvent()
    data class OperacaoAlterada(val operation: String) : OperationalEvent()
    object GpsError : OperationalEvent()
}
