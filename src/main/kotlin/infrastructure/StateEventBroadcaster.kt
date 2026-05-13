package com.example.infrastructure

import com.example.domain.StateChangedDto
import kotlinx.coroutines.flow.SharedFlow

interface StateEventBroadcaster {
    val events: SharedFlow<StateChangedDto>

    suspend fun emit(event: StateChangedDto)
}
