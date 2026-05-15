package com.example.presentation

import com.example.application.FsmService
import com.example.domain.EventRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.fsmRoutes(fsmService: FsmService) {
    route("/api") {
        post("/events/{entityId}") {
            val entityId = call.parameters["entityId"]!!
            val body = call.receive<EventRequest>()
            val result = fsmService.dispatch(entityId, body.event)
            call.respond(HttpStatusCode.OK, result)
        }

        get("/entities") {
            call.respond(HttpStatusCode.OK, fsmService.entities())
        }

        delete("/entities/{entityId}") {
            val entityId = call.parameters["entityId"]!!
            fsmService.removeEntity(entityId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
