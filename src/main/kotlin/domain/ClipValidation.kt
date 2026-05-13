package com.example.domain

/**
 * Validates a ClipCreateRequest and returns a list of validation errors.
 * An empty list means the request is valid.
 */
fun validateClipCreate(request: ClipCreateRequest): List<String> {
    val errors = mutableListOf<String>()
    if (request.name.isBlank()) errors += "name must not be blank"
    if (request.name.length > 255) errors += "name must not exceed 255 characters"
    if (request.durationMs < 1) errors += "durationMs must be >= 1"
    if (request.durationMs > 600_000) errors += "durationMs must be <= 600000"
    if (request.tags.size > 20) errors += "tags count must not exceed 20"
    request.tags.forEachIndexed { i, tag ->
        if (tag.length > 50) errors += "tag[$i] must not exceed 50 characters"
    }
    return errors
}

/**
 * Validates a ClipUpdateRequest and returns a list of validation errors.
 * An empty list means the request is valid.
 */
fun validateClipUpdate(request: ClipUpdateRequest): List<String> {
    val errors = mutableListOf<String>()
    if (request.name.isBlank()) errors += "name must not be blank"
    if (request.name.length > 255) errors += "name must not exceed 255 characters"
    if (request.durationMs < 1) errors += "durationMs must be >= 1"
    if (request.durationMs > 600_000) errors += "durationMs must be <= 600000"
    if (request.tags.size > 20) errors += "tags count must not exceed 20"
    request.tags.forEachIndexed { i, tag ->
        if (tag.length > 50) errors += "tag[$i] must not exceed 50 characters"
    }
    return errors
}
