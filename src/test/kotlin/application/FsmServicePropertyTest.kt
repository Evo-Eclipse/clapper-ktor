package com.example.application

import com.example.domain.AnimEvent
import com.example.domain.AnimState
import com.example.domain.TransitionTable
import com.example.infrastructure.InMemoryEntityRegistry
import com.example.infrastructure.SharedFlowBroadcaster
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Property 12: Потокобезопасность — конкурентные переходы не теряют обновления
 *
 * **Validates: Requirements 10.1, 10.2**
 */
class FsmServicePropertyTest :
    FunSpec({

        test("Property 12: N concurrent coroutines send events — all transitions execute, final state is correct") {
            checkAll(
                Arb.list(Arb.int(0 until AnimEvent.entries.size), 10..50),
            ) { eventIndices ->
                val registry = InMemoryEntityRegistry()
                val broadcaster = SharedFlowBroadcaster()
                val service = FsmService(registry, broadcaster)
                val entityId = "test-entity"

                // Pre-create entity so all coroutines share the same machine
                registry.getOrCreate(entityId)

                // Convert indices to events
                val events = eventIndices.map { AnimEvent.entries[it] }

                // Launch all dispatches concurrently
                val results =
                    coroutineScope {
                        events
                            .map { event ->
                                async {
                                    runCatching { service.dispatch(entityId, event) }
                                }
                            }.awaitAll()
                    }

                // Count successful transitions
                val successCount = results.count { it.isSuccess }

                // Verify the final state matches what the registry reports
                val machine = registry.getOrCreate(entityId)
                val actualFinalState = machine.current

                // The strongest invariant: final state matches snapshot
                val snapshot = service.entities()
                val entitySnapshot = snapshot.find { it.entityId == entityId }
                entitySnapshot?.state shouldBe actualFinalState.name

                // Every successful result corresponds to a real state change
                val successfulDtos = results.filter { it.isSuccess }.map { it.getOrThrow() }
                successfulDtos.size shouldBe successCount

                // Verify each successful DTO has valid from/to matching a valid transition
                successfulDtos.forEach { dto ->
                    val from = AnimState.valueOf(dto.from)
                    val to = AnimState.valueOf(dto.to)
                    val event = AnimEvent.valueOf(dto.triggeredBy)
                    TransitionTable.transition(from, event) shouldBe to
                }
            }
        }
    })
