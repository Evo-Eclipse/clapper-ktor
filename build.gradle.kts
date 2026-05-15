plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
ktlint {
    version = "1.6.0"
}

dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.routingOpenapi)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.swagger)
    implementation(ktorLibs.server.websockets)
    implementation(libs.flaxoos.ktor.server.rateLimiting)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
<<<<<<< New base: test(presentation): add integration tests for FSM routes
||||||| Common ancestor
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
=======
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(ktorLibs.client.websockets)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
>>>>>>> Current commit: test(presentation): add integration tests for FSM routes
}
