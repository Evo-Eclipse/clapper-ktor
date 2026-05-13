package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AnimEvent {
    MOVE,
    SPRINT,
    STOP,
    ATTACK_INPUT,
    HIT,
    RESPAWN,
}
