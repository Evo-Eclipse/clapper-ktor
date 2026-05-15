package com.example.presentation

import com.example.domain.AnimEvent
import com.example.domain.EntitySnapshotDto
import com.example.domain.EventRequest
import com.example.domain.StateChangedDto
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketTest {
    @Test
    fun `connect receives snapshot`() =
        testApp { services ->
            services.registry.getOrCreate("ws-entity")
            val client = wsClient()
            client.webSocket("/ws/state") {
                val snapshot = receiveDeserialized<List<EntitySnapshotDto>>()
                assertTrue(snapshot.any { it.entityId == "ws-entity" && it.state == "IDLE" })
            }
        }

    @Test
    fun `dispatch event receives StateChangedDto via WS`() =
        testApp { services ->
            services.registry.getOrCreate("ws-hero")
            val wsClientInstance = wsClient()
            val httpClient = jsonClient()

            wsClientInstance.webSocket("/ws/state") {
                // Receive initial snapshot
                receiveDeserialized<List<EntitySnapshotDto>>()

                // Give the collector time to subscribe
                val sendJob =
                    async {
                        delay(100)
                        httpClient.post("/api/events/ws-hero") {
                            contentType(ContentType.Application.Json)
                            setBody(EventRequest(event = AnimEvent.MOVE))
                        }
                    }

                val dto = receiveDeserialized<StateChangedDto>()
                assertEquals("ws-hero", dto.entityId)
                assertEquals("IDLE", dto.from)
                assertEquals("WALK", dto.to)
                assertEquals("MOVE", dto.triggeredBy)

                sendJob.await()
            }
        }

    @Test
    fun `delete entity receives REMOVED via WS`() =
        testApp { services ->
            services.registry.getOrCreate("ws-delete")
            val wsClientInstance = wsClient()
            val httpClient = jsonClient()

            wsClientInstance.webSocket("/ws/state") {
                // Receive initial snapshot
                receiveDeserialized<List<EntitySnapshotDto>>()

                val sendJob =
                    async {
                        delay(100)
                        httpClient.delete("/api/entities/ws-delete")
                    }

                val dto = receiveDeserialized<StateChangedDto>()
                assertEquals("ws-delete", dto.entityId)
                assertEquals("IDLE", dto.from)
                assertEquals("REMOVED", dto.to)
                assertEquals("DELETE", dto.triggeredBy)

                sendJob.await()
            }
        }
}
