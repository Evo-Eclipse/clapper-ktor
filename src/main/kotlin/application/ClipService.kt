package com.example.application

import com.example.domain.AnimState
import com.example.domain.AnimationClip
import com.example.domain.ClipCreateRequest
import com.example.domain.ClipNotFoundException
import com.example.domain.ClipUpdateRequest
import com.example.domain.InvalidClipDataException
import com.example.domain.validateClipCreate
import com.example.domain.validateClipUpdate
import com.example.infrastructure.ClipStore

class ClipService(
    private val store: ClipStore,
) {
    fun create(request: ClipCreateRequest): AnimationClip {
        val errors = validateClipCreate(request)
        if (errors.isNotEmpty()) throw InvalidClipDataException(errors)
        return store.create(request.toAnimationClip())
    }

    fun getAll(): List<AnimationClip> = store.getAll()

    fun getByState(state: AnimState): List<AnimationClip> = store.getByState(state)

    fun getById(id: String): AnimationClip = store.getById(id) ?: throw ClipNotFoundException(id)

    fun update(
        id: String,
        request: ClipUpdateRequest,
    ): AnimationClip {
        val errors = validateClipUpdate(request)
        if (errors.isNotEmpty()) throw InvalidClipDataException(errors)
        return store.update(id, request.toAnimationClip())
            ?: throw ClipNotFoundException(id)
    }

    fun delete(id: String) {
        require(isValidUuid(id)) { "Invalid UUID format: $id" }
        if (!store.delete(id)) throw ClipNotFoundException(id)
    }

    private fun isValidUuid(value: String): Boolean = UUID_REGEX.matches(value)

    companion object {
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
