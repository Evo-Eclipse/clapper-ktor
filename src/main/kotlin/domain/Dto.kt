package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
data class StateChangedDto(
    val entityId: String,
    val from: String,
    val to: String,
    val triggeredBy: String,
    val timestamp: Long,
)

@Serializable
data class EntitySnapshotDto(
    val entityId: String,
    val state: String,
)

@Serializable
data class AnimationClip(
    val id: String = "",
    val name: String,
    val state: AnimState,
    val durationMs: Int,
    val loop: Boolean,
    val tags: List<String> = emptyList(),
)

@Serializable
data class EventRequest(
    val event: AnimEvent,
)

@Serializable
data class ClipCreateRequest(
    val name: String,
    val state: AnimState,
    val durationMs: Int,
    val loop: Boolean,
    val tags: List<String> = emptyList(),
) {
    fun toAnimationClip() =
        AnimationClip(
            name = name,
            state = state,
            durationMs = durationMs,
            loop = loop,
            tags = tags,
        )
}

@Serializable
data class ClipUpdateRequest(
    val name: String,
    val state: AnimState,
    val durationMs: Int,
    val loop: Boolean,
    val tags: List<String> = emptyList(),
) {
    fun toAnimationClip() =
        AnimationClip(
            name = name,
            state = state,
            durationMs = durationMs,
            loop = loop,
            tags = tags,
        )
}

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null,
)
