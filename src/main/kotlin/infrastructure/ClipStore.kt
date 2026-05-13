package com.example.infrastructure

import com.example.domain.AnimState
import com.example.domain.AnimationClip

interface ClipStore {
    fun create(clip: AnimationClip): AnimationClip

    fun getAll(): List<AnimationClip>

    fun getByState(state: AnimState): List<AnimationClip>

    fun getById(id: String): AnimationClip?

    fun update(
        id: String,
        clip: AnimationClip,
    ): AnimationClip?

    fun delete(id: String): Boolean
}
