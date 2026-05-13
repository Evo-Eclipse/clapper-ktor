package com.example.infrastructure

import com.example.domain.StateChangedDto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SharedFlowBroadcaster : StateEventBroadcaster {
    private val _events =
        MutableSharedFlow<StateChangedDto>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val events: SharedFlow<StateChangedDto> = _events.asSharedFlow()

    override suspend fun emit(event: StateChangedDto) {
        _events.emit(event)
    }
}
