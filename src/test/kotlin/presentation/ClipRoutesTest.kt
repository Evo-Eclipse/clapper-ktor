package com.example.presentation

import com.example.domain.AnimState
import com.example.domain.AnimationClip
import com.example.domain.ClipCreateRequest
import com.example.domain.ClipUpdateRequest
import com.example.domain.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipRoutesTest {
    @Test
    fun `full CRUD cycle for clips`() =
        testApp {
            val client = jsonClient()

            // Create
            val createResponse =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "Walk Animation",
                            state = AnimState.WALK,
                            durationMs = 1000,
                            loop = true,
                            tags = listOf("movement"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Created, createResponse.status)
            val created = createResponse.body<AnimationClip>()
            assertTrue(created.id.isNotBlank())
            assertEquals("Walk Animation", created.name)
            assertEquals(AnimState.WALK, created.state)

            // Get by ID
            val getResponse = client.get("/api/clips/${created.id}")
            assertEquals(HttpStatusCode.OK, getResponse.status)
            val fetched = getResponse.body<AnimationClip>()
            assertEquals(created, fetched)

            // Update
            val updateResponse =
                client.put("/api/clips/${created.id}") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipUpdateRequest(
                            name = "Run Animation",
                            state = AnimState.RUN,
                            durationMs = 500,
                            loop = false,
                            tags = listOf("movement", "fast"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, updateResponse.status)
            val updated = updateResponse.body<AnimationClip>()
            assertEquals(created.id, updated.id)
            assertEquals("Run Animation", updated.name)
            assertEquals(AnimState.RUN, updated.state)
            assertEquals(500, updated.durationMs)

            // Get after update
            val getAfterUpdate = client.get("/api/clips/${created.id}")
            assertEquals(HttpStatusCode.OK, getAfterUpdate.status)
            val fetchedAfterUpdate = getAfterUpdate.body<AnimationClip>()
            assertEquals(updated, fetchedAfterUpdate)

            // Delete
            val deleteResponse = client.delete("/api/clips/${created.id}")
            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // Get after delete → 404
            val getAfterDelete = client.get("/api/clips/${created.id}")
            assertEquals(HttpStatusCode.NotFound, getAfterDelete.status)
        }

    @Test
    fun `POST invalid clip returns 400`() =
        testApp {
            val client = jsonClient()
            val response =
                client.post("/api/clips") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ClipCreateRequest(
                            name = "",
                            state = AnimState.IDLE,
                            durationMs = 0,
                            loop = false,
                            tags = emptyList(),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("Validation failed", error.error)
        }

    @Test
    fun `GET non-existing clip returns 404`() =
        testApp {
            val client = jsonClient()
            val response = client.get("/api/clips/00000000-0000-0000-0000-000000000000")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET clips with invalid state query param returns 400`() =
        testApp {
            val client = jsonClient()
            val response = client.get("/api/clips?state=INVALID_STATE")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("Invalid parameter", error.error)
        }

    @Test
    fun `DELETE clip with invalid UUID returns 400`() =
        testApp {
            val client = jsonClient()
            val response = client.delete("/api/clips/not-a-valid-uuid")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("Invalid parameter", error.error)
        }
}
