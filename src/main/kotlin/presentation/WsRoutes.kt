package com.example.presentation

import com.example.infrastructure.EntityRegistry
import com.example.infrastructure.StateEventBroadcaster
import io.ktor.server.routing.Route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

fun Route.wsRoutes(
    broadcaster: StateEventBroadcaster,
    registry: EntityRegistry,
) {
    webSocket("/ws/state") {
        val snapshot = registry.snapshot()
        sendSerialized(snapshot)

        broadcaster.events.collect { event ->
            sendSerialized(event)
        }
    }
}
