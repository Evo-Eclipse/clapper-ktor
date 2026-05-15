package com.example

import com.example.application.configureDependencies
import com.example.presentation.clipRoutes
import com.example.presentation.configureStatusPages
import com.example.presentation.fsmRoutes
import com.example.presentation.wsRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    val container = configureDependencies()

    // Monitoring plugins
    install(CallLogging) {
        callIdMdc("call-id")
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String -> callId.isNotEmpty() }
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }

    // Content negotiation
    install(ContentNegotiation) {
        json()
    }

    // WebSockets
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    // Error handling
    configureStatusPages()

    // Routing
    routing {
        fsmRoutes(container.fsmService)
        clipRoutes(container.clipService)
        wsRoutes(container.broadcaster, container.entityRegistry)

        // Swagger UI
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")

        // OpenAPI spec endpoint
        get("/api/openapi.json") {
            val spec =
                this::class.java.classLoader
                    .getResource("documentation.yaml")
                    ?.readText()
                    ?: error("OpenAPI spec not found")
            call.respondText(spec, ContentType.Application.Json)
        }
    }
}
