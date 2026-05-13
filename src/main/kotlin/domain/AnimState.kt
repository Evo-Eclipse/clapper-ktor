package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AnimState {
    IDLE,
    WALK,
    RUN,
    ATTACK,
    DEATH,
}
