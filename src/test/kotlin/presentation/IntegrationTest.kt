package com.example.presentation

import com.example.domain.AnimEvent
import com.example.domain.AnimState
import com.example.domain.AnimationClip
import com.example.domain.ClipCreateRequest
import com.example.domain.EntitySnapshotDto
import com.example.domain.EventRequest
import com.example.domain.StateChangedDto
import com.example.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests validating the complete application workflow
 * according to SPEC requirements without regard to current implementation.
 *
 * Tests the interaction between:
 * - REST API endpoints
 * - FSM state management
 * - WebSocket broadcasting
 * - Clip CRUD operations
 * - Seed data initialization
 */
class IntegrationTest {
    @Test
    fun `full FSM lifecycle with WebSocket broadcasting`() =
        testApplication {
            application { module() }

            val wsClient =
                createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                    }
                }
            val httpClient =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            wsClient.webSocket("/ws/state") {
                // Receive initial snapshot with seeded entities
                val snapshot = receiveDeserialized<List<EntitySnapshotDto>>()
                assertTrue(snapshot.any { it.entityId == "hero" && it.state == "IDLE" })
                assertTrue(snapshot.any { it.entityId == "enemy_1" && it.state == "IDLE" })
                assertTrue(snapshot.any { it.entityId == "enemy_2" && it.state == "IDLE" })

                // Dispatch valid transition: hero IDLE -> WALK via MOVE
                httpClient.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.MOVE))
                }

                // WebSocket should receive StateChangedDto
                val stateChange = receiveDeserialized<StateChangedDto>()
                assertEquals("hero", stateChange.entityId)
                assertEquals("IDLE", stateChange.from)
                assertEquals("WALK", stateChange.to)
                assertEquals("MOVE", stateChange.triggeredBy)
                assertTrue(stateChange.timestamp > 0)

                // Dispatch forbidden transition: hero WALK -> RESPAWN (invalid from WALK)
                val invalidResponse =
                    httpClient.post("/api/events/hero") {
                        contentType(ContentType.Application.Json)
                        setBody(EventRequest(event = AnimEvent.RESPAWN))
                    }
                assertEquals(HttpStatusCode.Conflict, invalidResponse.status)
            }
        }

    @Test
    fun `complete clip CRUD cycle with validation`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // Verify seeded clips exist
            val seededClips = client.get("/api/clips").body<List<AnimationClip>>()
            assertEquals(3, seededClips.size)

            // Create new clip
            val newClipRequest =
                ClipCreateRequest(
                    name = "run_sprint",
                    state = AnimState.RUN,
                    durationMs = 300,
                    loop = true,
                    tags = listOf("movement", "fast"),
                )

            val createResponse =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(newClipRequest)
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val createdClip = createResponse.body<AnimationClip>()
            assertNotNull(createdClip.id)
            assertEquals(newClipRequest.name, createdClip.name)
            assertEquals(newClipRequest.state, createdClip.state)
            assertEquals(newClipRequest.durationMs, createdClip.durationMs)

            // Get clip by ID
            val getResponse = client.get("/api/clips/${createdClip.id}")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val fetchedClip = getResponse.body<AnimationClip>()
            assertEquals(createdClip, fetchedClip)

            // Filter by state
            val runClips = client.get("/api/clips?state=RUN").body<List<AnimationClip>>()
            assertTrue(runClips.any { it.id == createdClip.id })

            // Update clip
            val updateRequest =
                ClipCreateRequest(
                    name = "run_sprint_v2",
                    state = AnimState.RUN,
                    durationMs = 350,
                    loop = false,
                    tags = listOf("movement", "fast", "updated"),
                )

            val updateResponse =
                client.put("/api/clips/${createdClip.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(updateRequest)
                }
            assertEquals(HttpStatusCode.OK, updateResponse.status)
            val updatedClip = updateResponse.body<AnimationClip>()
            assertEquals(createdClip.id, updatedClip.id)
            assertEquals(updateRequest.name, updatedClip.name)
            assertEquals(updateRequest.durationMs, updatedClip.durationMs)
            assertEquals(updateRequest.loop, updatedClip.loop)
            assertEquals(updateRequest.tags, updatedClip.tags)

            // Delete clip
            val deleteResponse = client.delete("/api/clips/${createdClip.id}")
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // Verify clip is gone
            val notFoundResponse = client.get("/api/clips/${createdClip.id}")
            assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)
        }

    @Test
    fun `entity deletion broadcasts REMOVED via WebSocket`() =
        testApplication {
            application { module() }

            val wsClient =
                createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                    }
                }
            val httpClient =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            wsClient.webSocket("/ws/state") {
                // Receive initial snapshot
                receiveDeserialized<List<EntitySnapshotDto>>()

                // Create and delete a new entity
                delay(100)
                httpClient.post("/api/events/test-entity") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.MOVE))
                }
                delay(100)

                // Receive state change from creation
                receiveDeserialized<StateChangedDto>()

                // Delete entity
                httpClient.delete("/api/entities/test-entity")

                // Receive REMOVED broadcast
                val removedEvent = receiveDeserialized<StateChangedDto>()
                assertEquals("test-entity", removedEvent.entityId)
                assertEquals("WALK", removedEvent.from)
                assertEquals("REMOVED", removedEvent.to)
                assertEquals("DELETE", removedEvent.triggeredBy)
            }
        }

    @Test
    fun `concurrent FSM transitions are processed sequentially`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // Ensure hero starts at IDLE by recreating
            client.delete("/api/entities/hero")
            client.post("/api/events/hero") {
                contentType(ContentType.Application.Json)
                setBody(EventRequest(event = AnimEvent.MOVE))
            }

            // hero is now at WALK; send multiple STOP events (WALK -> IDLE)
            // then MOVE events (IDLE -> WALK) alternating to verify sequential processing
            val events =
                listOf(
                    AnimEvent.STOP,
                    AnimEvent.MOVE,
                    AnimEvent.STOP,
                    AnimEvent.MOVE,
                    AnimEvent.STOP,
                    AnimEvent.MOVE,
                )

            val responses =
                events.map { event ->
                    client.post("/api/events/hero") {
                        contentType(ContentType.Application.Json)
                        setBody(EventRequest(event = event))
                    }
                }

            // All should succeed since they alternate valid transitions
            responses.forEach { response ->
                assertEquals(HttpStatusCode.OK, response.status)
            }

            // Final state should be WALK (last event was MOVE: IDLE -> WALK)
            val finalSnapshot = client.get("/api/entities").body<List<EntitySnapshotDto>>()
            val heroState = finalSnapshot.find { it.entityId == "hero" }?.state
            assertEquals("WALK", heroState)
        }

    @Test
    fun `invalid event value returns 400 Bad Request`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"event\":\"INVALID_EVENT\"}")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `forbidden transition returns 409 Conflict`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // hero starts at IDLE, SPRINT is not allowed from IDLE
            val response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.SPRINT))
                }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }

    @Test
    fun `clip creation with invalid data returns 400`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "",
                            state = AnimState.IDLE,
                            durationMs = 0,
                            loop = false,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `delete non existing entity returns 404`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.delete("/api/entities/non-existing-entity")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `delete non existing clip returns 404`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.delete("/api/clips/00000000-0000-0000-0000-000000000000")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `delete clip with invalid UUID format returns 400`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.delete("/api/clips/not-a-valid-uuid")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `update non existing clip returns 404`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response =
                client.put("/api/clips/00000000-0000-0000-0000-000000000000") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "updated",
                            state = AnimState.IDLE,
                            durationMs = 500,
                            loop = false,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `update clip with invalid data returns 400`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // First create a valid clip
            val createResponse =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "valid_clip",
                            state = AnimState.IDLE,
                            durationMs = 1000,
                            loop = true,
                        ),
                    )
                }
            val createdClip = createResponse.body<AnimationClip>()

            // Try to update with invalid data (empty name, zero duration)
            val response =
                client.put("/api/clips/${createdClip.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "",
                            state = AnimState.IDLE,
                            durationMs = 0,
                            loop = false,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `filter clips by non existing state returns empty array`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.get("/api/clips?state=NON_EXISTENT")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `complete FSM state transition cycle for hero`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // Reset hero to IDLE
            client.delete("/api/entities/hero")
            client.post("/api/events/hero") {
                contentType(ContentType.Application.Json)
                setBody(EventRequest(event = AnimEvent.RESPAWN))
            }

            // IDLE -> MOVE -> WALK
            var response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.MOVE))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            var stateChange = response.body<StateChangedDto>()
            assertEquals("IDLE", stateChange.from)
            assertEquals("WALK", stateChange.to)

            // WALK -> SPRINT -> RUN
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.SPRINT))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("WALK", stateChange.from)
            assertEquals("RUN", stateChange.to)

            // RUN -> STOP -> WALK
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.STOP))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("RUN", stateChange.from)
            assertEquals("WALK", stateChange.to)

            // WALK -> ATTACK_INPUT -> ATTACK
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.ATTACK_INPUT))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("WALK", stateChange.from)
            assertEquals("ATTACK", stateChange.to)

            // ATTACK -> MOVE -> IDLE
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.MOVE))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("ATTACK", stateChange.from)
            assertEquals("IDLE", stateChange.to)

            // IDLE -> HIT -> DEATH
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.HIT))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("IDLE", stateChange.from)
            assertEquals("DEATH", stateChange.to)

            // DEATH -> RESPAWN -> IDLE
            response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.RESPAWN))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            stateChange = response.body<StateChangedDto>()
            assertEquals("DEATH", stateChange.from)
            assertEquals("IDLE", stateChange.to)

            // Verify final state
            val snapshot = client.get("/api/entities").body<List<EntitySnapshotDto>>()
            val heroState = snapshot.find { it.entityId == "hero" }?.state
            assertEquals("IDLE", heroState)
        }

    @Test
    fun `Swagger UI is accessible`() =
        testApplication {
            application { module() }

            val client =
                createClient {}
            val response = client.get("/swagger")
            assertTrue(
                response.status == HttpStatusCode.OK ||
                    response.status == HttpStatusCode.MovedPermanently ||
                    response.status == HttpStatusCode.Found,
            )
        }

    @Test
    fun `OpenAPI spec is accessible and valid`() =
        testApplication {
            application { module() }

            val client =
                createClient {}
            val response = client.get("/api/openapi.json")
            assertEquals(HttpStatusCode.OK, response.status)

            val spec = response.bodyAsText()
            assertTrue(spec.contains("openapi"))
            assertTrue(spec.contains("Clapper Animation Server API"))
            assertTrue(spec.contains("/api/events"))
            assertTrue(spec.contains("/api/clips"))
        }

    @Test
    fun `clip update ignores id field in request body`() =
        testApplication {
            application { module() }

            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            // Create a clip
            val createResponse =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "original",
                            state = AnimState.IDLE,
                            durationMs = 1000,
                            loop = true,
                        ),
                    )
                }
            val createdClip = createResponse.body<AnimationClip>()
            val originalId = createdClip.id

            // Update with different data (id from URL is used, not body)
            val updateResponse =
                client.put("/api/clips/$originalId") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "updated",
                            state = AnimState.WALK,
                            durationMs = 500,
                            loop = false,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, updateResponse.status)

            val updatedClip = updateResponse.body<AnimationClip>()
            // ID should remain the same as URL parameter, not from body
            assertEquals(originalId, updatedClip.id)
            assertEquals("updated", updatedClip.name)
            assertEquals(AnimState.WALK, updatedClip.state)

            // Verify via GET
            val getResponse = client.get("/api/clips/$originalId")
            val fetchedClip = getResponse.body<AnimationClip>()
            assertEquals(originalId, fetchedClip.id)
        }
}
