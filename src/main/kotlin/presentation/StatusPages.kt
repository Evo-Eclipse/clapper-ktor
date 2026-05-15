package com.example.presentation

import com.example.domain.ClipNotFoundException
import com.example.domain.EntityNotFoundException
import com.example.domain.ErrorResponse
import com.example.domain.InvalidClipDataException
import com.example.domain.InvalidTransitionException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<InvalidTransitionException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(
                    error = "Forbidden transition",
                    details = "Entity '${cause.entityId}': cannot transition" +
                        " from ${cause.currentState} by ${cause.event}",
                ),
            )
        }

        exception<EntityNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = "Entity not found",
                    details = "Entity '${cause.entityId}' does not exist",
                ),
            )
        }

        exception<ClipNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = "Clip not found",
                    details = "Clip '${cause.clipId}' does not exist",
                ),
            )
        }

        exception<InvalidClipDataException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Validation failed",
                    details = cause.reasons.joinToString("; "),
                ),
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Invalid request body",
                    details = cause.message ?: "Malformed request",
                ),
            )
        }

        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Invalid request body",
                    details = cause.message ?: "Malformed JSON",
                ),
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "Invalid parameter",
                    details = cause.message ?: "Invalid value",
                ),
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Internal server error"),
            )
        }
    }
}
