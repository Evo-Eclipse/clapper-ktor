package com.example.presentation

import com.example.application.ClipService
import com.example.domain.AnimState
import com.example.domain.ClipCreateRequest
import com.example.domain.ClipUpdateRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.clipRoutes(clipService: ClipService) {
    route("/api/clips") {
        post {
            val body = call.receive<ClipCreateRequest>()
            val clip = clipService.create(body)
            call.respond(HttpStatusCode.Created, clip)
        }

        get {
            val stateParam = call.request.queryParameters["state"]
            val clips =
                if (stateParam != null) {
                    val state = AnimState.valueOf(stateParam)
                    clipService.getByState(state)
                } else {
                    clipService.getAll()
                }
            call.respond(HttpStatusCode.OK, clips)
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            call.respond(HttpStatusCode.OK, clipService.getById(id))
        }

        put("/{id}") {
            val id = call.parameters["id"]!!
            val body = call.receive<ClipUpdateRequest>()
            val updated = clipService.update(id, body)
            call.respond(HttpStatusCode.OK, updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]!!
            clipService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
