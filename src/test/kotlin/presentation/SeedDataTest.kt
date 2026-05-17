package com.example.presentation

import com.example.domain.AnimationClip
import com.example.domain.EntitySnapshotDto
import com.example.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedDataTest {
    @Test
    fun `GET api entities returns seeded entities in IDLE state`() =
        testApplication {
            application { module() }
            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.get("/api/entities")
            assertEquals(HttpStatusCode.OK, response.status)

            val entities = response.body<List<EntitySnapshotDto>>()
            val entityIds = entities.map { it.entityId }.toSet()
            assertTrue(entityIds.contains("hero"), "Should contain hero")
            assertTrue(entityIds.contains("enemy_1"), "Should contain enemy_1")
            assertTrue(entityIds.contains("enemy_2"), "Should contain enemy_2")
            entities.forEach { entity ->
                assertEquals("IDLE", entity.state, "Entity ${entity.entityId} should be IDLE")
            }
        }

    @Test
    fun `GET api clips returns 3 seeded clips`() =
        testApplication {
            application { module() }
            val client =
                createClient {
                    install(ContentNegotiation) { json() }
                }

            val response = client.get("/api/clips")
            assertEquals(HttpStatusCode.OK, response.status)

            val clips = response.body<List<AnimationClip>>()
            assertEquals(3, clips.size, "Should have 3 seeded clips")

            val clipNames = clips.map { it.name }.toSet()
            assertTrue(clipNames.contains("idle_breathing"))
            assertTrue(clipNames.contains("walk_cycle"))
            assertTrue(clipNames.contains("attack_slash"))
        }

    @Test
    fun `GET swagger returns 200`() =
        testApplication {
            application { module() }

            val response = client.get("/swagger")
            assertTrue(
                response.status == HttpStatusCode.OK ||
                    response.status == HttpStatusCode.MovedPermanently ||
                    response.status == HttpStatusCode.Found,
                "Swagger should return 200 or redirect, got ${response.status}",
            )
        }

    @Test
    fun `GET api openapi json returns 200 with spec content`() =
        testApplication {
            application { module() }

            val response = client.get("/api/openapi.json")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("openapi"), "Response should contain OpenAPI spec")
            assertTrue(body.contains("/api/events"), "Spec should document events endpoint")
            assertTrue(body.contains("/api/clips"), "Spec should document clips endpoint")
        }
}
