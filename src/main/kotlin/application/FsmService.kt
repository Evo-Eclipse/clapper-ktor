package com.example.application

import com.example.domain.AnimEvent
import com.example.domain.EntityNotFoundException
import com.example.domain.StateChangedDto
import com.example.infrastructure.EntityRegistry
import com.example.infrastructure.StateEventBroadcaster

class FsmService(
    private val registry: EntityRegistry,
    private val broadcaster: StateEventBroadcaster,
) {
    suspend fun dispatch(
        entityId: String,
        event: AnimEvent,
    ): StateChangedDto {
        val machine = registry.getOrCreate(entityId)
        val (from, to) =
            synchronized(machine) {
                machine.process(event).getOrThrow()
            }
        val dto =
            StateChangedDto(
                entityId = entityId,
                from = from.name,
                to = to.name,
                triggeredBy = event.name,
                timestamp = System.currentTimeMillis(),
            )
        broadcaster.emit(dto)
        return dto
    }

    fun entities() = registry.snapshot()

    suspend fun removeEntity(entityId: String): StateChangedDto {
        val machine =
            registry.remove(entityId)
                ?: throw EntityNotFoundException(entityId)
        val dto =
            StateChangedDto(
                entityId = entityId,
                from = machine.current.name,
                to = "REMOVED",
                triggeredBy = "DELETE",
                timestamp = System.currentTimeMillis(),
            )
        broadcaster.emit(dto)
        return dto
    }
}
