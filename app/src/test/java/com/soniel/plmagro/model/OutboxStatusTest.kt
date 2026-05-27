package com.soniel.plmagro.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OutboxStatusTest {
    @Test
    fun newOutboxEventsStartAsPendente() {
        val event = OutboxEventEntity(
            tipoEvento = "OPERACAO",
            payloadJson = "{}",
            vehicleId = "FROTA-LOCAL"
        )

        assertEquals(OutboxStatus.PENDENTE, event.syncStatus)
    }
}
