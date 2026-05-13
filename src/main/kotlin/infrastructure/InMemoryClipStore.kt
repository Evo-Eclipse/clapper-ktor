package com.example.infrastructure

import com.example.domain.AnimState
import com.example.domain.AnimationClip
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryClipStore : ClipStore {
    private val clips = ConcurrentHashMap<String, AnimationClip>()

    override fun create(clip: AnimationClip): AnimationClip {
        val withId = clip.copy(id = UUID.randomUUID().toString())
        clips[withId.id] = withId
        return withId
    }

    override fun getAll(): List<AnimationClip> = clips.values.toList()

    override fun getByState(state: AnimState): List<AnimationClip> =
        clips.values.filter { it.state == state }

    override fun getById(id: String): AnimationClip? = clips[id]

    override fun update(
        id: String,
        clip: AnimationClip,
    ): AnimationClip? {
        if (!clips.containsKey(id)) return null
        val updated = clip.copy(id = id)
        clips[id] = updated
        return updated
    }

    override fun delete(id: String): Boolean = clips.remove(id) != null
}
