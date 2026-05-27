package com.soniel.plmagro.core.eventbus

import com.soniel.plmagro.model.OperationalEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object OperationalEventBus {
    private val _events = MutableSharedFlow<OperationalEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    suspend fun emit(event: OperationalEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: OperationalEvent): Boolean {
        return _events.tryEmit(event)
    }
}
