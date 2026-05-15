package com.example.presentation

import com.example.domain.AnimEvent
import com.example.domain.EntitySnapshotDto
import com.example.domain.ErrorResponse
import com.example.domain.EventRequest
import com.example.domain.StateChangedDto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsmRoutesTest {
    @Test
    fun `POST valid event returns 200 with StateChangedDto`() =
        testApp {
            val client = jsonClient()
            val response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.MOVE))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val dto = response.body<StateChangedDto>()
            assertEquals("hero", dto.entityId)
            assertEquals("IDLE", dto.from)
            assertEquals("WALK", dto.to)
            assertEquals("MOVE", dto.triggeredBy)
            assertTrue(dto.timestamp > 0)
        }

    @Test
    fun `POST invalid event returns 400`() =
        testApp {
            val client =
                createClient {
                }
            val response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"event":"INVALID_EVENT"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST forbidden transition returns 409`() =
        testApp {
            val client = jsonClient()
            // Entity starts in IDLE, SPRINT is not allowed from IDLE
            val response =
                client.post("/api/events/hero") {
                    contentType(ContentType.Application.Json)
                    setBody(EventRequest(event = AnimEvent.SPRINT))
                }
            assertEquals(HttpStatusCode.Conflict, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("Forbidden transition", error.error)
        }

    @Test
    fun `GET entities returns 200 with array`() =
        testApp { services ->
            val client = jsonClient()
            // Seed an entity
            services.registry.getOrCreate("test-entity")
            val response = client.get("/api/entities")
            assertEquals(HttpStatusCode.OK, response.status)
            val entities = response.body<List<EntitySnapshotDto>>()
            assertTrue(entities.any { it.entityId == "test-entity" })
        }

    @Test
    fun `DELETE existing entity returns 204`() =
        testApp { services ->
            val client = jsonClient()
            services.registry.getOrCreate("to-delete")
            val response = client.delete("/api/entities/to-delete")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }

    @Test
    fun `DELETE non-existing entity returns 404`() =
        testApp {
            val client = jsonClient()
            val response = client.delete("/api/entities/non-existing")
            assertEquals(HttpStatusCode.NotFound, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("Entity not found", error.error)
        }
}
