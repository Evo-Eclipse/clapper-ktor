package com.example.infrastructure

import com.example.domain.EntitySnapshotDto
import com.example.domain.StateMachine

interface EntityRegistry {
    fun getOrCreate(entityId: String): StateMachine

    fun get(entityId: String): StateMachine?

    fun remove(entityId: String): StateMachine?

    fun snapshot(): List<EntitySnapshotDto>
}
