package com.example.presentation

import com.example.application.ClipService
import com.example.application.FsmService
import com.example.infrastructure.InMemoryClipStore
import com.example.infrastructure.InMemoryEntityRegistry
import com.example.infrastructure.SharedFlowBroadcaster
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.websocket.WebSockets as ServerWebSockets

fun testApp(block: suspend ApplicationTestBuilder.(TestServices) -> Unit) =
    testApplication {
        val registry = InMemoryEntityRegistry()
        val clipStore = InMemoryClipStore()
        val broadcaster = SharedFlowBroadcaster()
        val fsmService = FsmService(registry, broadcaster)
        val clipService = ClipService(clipStore)
        val services = TestServices(fsmService, clipService, broadcaster, registry)

        application {
            testModule(fsmService, clipService, broadcaster, registry)
        }

        block(services)
    }

fun Application.testModule(
    fsmService: FsmService,
    clipService: ClipService,
    broadcaster: SharedFlowBroadcaster,
    registry: InMemoryEntityRegistry,
) {
    configureStatusPages()
    install(ServerContentNegotiation) {
        json()
    }
    install(ServerWebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        fsmRoutes(fsmService)
        clipRoutes(clipService)
        wsRoutes(broadcaster, registry)
    }
}

fun ApplicationTestBuilder.jsonClient(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json()
        }
    }

fun ApplicationTestBuilder.wsClient(): HttpClient =
    createClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

data class TestServices(
    val fsmService: FsmService,
    val clipService: ClipService,
    val broadcaster: SharedFlowBroadcaster,
    val registry: InMemoryEntityRegistry,
)
