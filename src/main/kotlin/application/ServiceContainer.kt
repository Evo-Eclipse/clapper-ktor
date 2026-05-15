package com.example.application

import com.example.domain.AnimState
import com.example.domain.AnimationClip
import com.example.infrastructure.ClipStore
import com.example.infrastructure.EntityRegistry
import com.example.infrastructure.InMemoryClipStore
import com.example.infrastructure.InMemoryEntityRegistry
import com.example.infrastructure.SharedFlowBroadcaster
import com.example.infrastructure.StateEventBroadcaster
import io.ktor.server.application.Application

data class ServiceContainer(
    val fsmService: FsmService,
    val clipService: ClipService,
    val broadcaster: StateEventBroadcaster,
    val entityRegistry: EntityRegistry,
)

fun Application.configureDependencies(): ServiceContainer {
    val entityRegistry: EntityRegistry = InMemoryEntityRegistry()
    val clipStore: ClipStore = InMemoryClipStore()
    val broadcaster: StateEventBroadcaster = SharedFlowBroadcaster()

    val fsmService = FsmService(entityRegistry, broadcaster)
    val clipService = ClipService(clipStore)

    // Seed entities
    entityRegistry.getOrCreate("hero")
    entityRegistry.getOrCreate("enemy_1")
    entityRegistry.getOrCreate("enemy_2")

    // Seed clips
    clipStore.create(
        AnimationClip(
            name = "idle_breathing",
            state = AnimState.IDLE,
            durationMs = 2000,
            loop = true,
            tags = listOf("idle", "breathing"),
        ),
    )
    clipStore.create(
        AnimationClip(
            name = "walk_cycle",
            state = AnimState.WALK,
            durationMs = 800,
            loop = true,
            tags = listOf("movement", "walk"),
        ),
    )
    clipStore.create(
        AnimationClip(
            name = "attack_slash",
            state = AnimState.ATTACK,
            durationMs = 600,
            loop = false,
            tags = listOf("combat", "melee"),
        ),
    )

    return ServiceContainer(fsmService, clipService, broadcaster, entityRegistry)
}
