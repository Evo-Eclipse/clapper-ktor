package com.example.infrastructure

import com.example.domain.EntitySnapshotDto
import com.example.domain.StateMachine
import java.util.concurrent.ConcurrentHashMap

class InMemoryEntityRegistry : EntityRegistry {
    private val entities = ConcurrentHashMap<String, StateMachine>()

    override fun getOrCreate(entityId: String): StateMachine =
        entities.computeIfAbsent(entityId) { StateMachine(it) }

    override fun get(entityId: String): StateMachine? = entities[entityId]

    override fun remove(entityId: String): StateMachine? = entities.remove(entityId)

    override fun snapshot(): List<EntitySnapshotDto> =
        entities.values.map { EntitySnapshotDto(it.entityId, it.current.name) }
}
