package com.example.infrastructure

import com.example.domain.AnimState
import com.example.domain.TransitionTable
import com.example.domain.validTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for EntityRegistry.
 *
 * Validates: Requirements 3.1, 4.1, 4.2
 */
class EntityRegistryPropertyTest :
    FunSpec({

        test(
            "Property 4: Снимок отражает актуальное состояние — " +
                "after a series of transitions, snapshot() reflects the last state of each entity",
        ) {
            /**
             * Validates: Requirements 3.1
             *
             * For any sequence of valid transitions applied to a set of entities,
             * snapshot() SHALL return a list where each entity's state corresponds
             * to the result of the last successful transition (or IDLE if no transitions).
             */
            checkAll(
                Arb.string(1..20),
                Arb.list(Arb.validTransition(), 1..10),
            ) { entityId, transitions ->
                val registry = InMemoryEntityRegistry()
                val machine = registry.getOrCreate(entityId)

                var expectedState = AnimState.IDLE
                for ((_, event) in transitions) {
                    val target = TransitionTable.transition(expectedState, event)
                    if (target != null) {
                        machine.process(event)
                        expectedState = target
                    }
                }

                val snapshot = registry.snapshot()
                val entitySnapshot = snapshot.find { it.entityId == entityId }
                entitySnapshot?.state shouldBe expectedState.name
            }
        }

        test(
            "Property 5: Удаление сущности удаляет из реестра — " +
                "after remove(), entity is absent from snapshot()",
        ) {
            /**
             * Validates: Requirements 4.1, 4.2
             *
             * For any existing entity with entityId, after removal:
             * (a) entity SHALL be absent from snapshot()
             */
            checkAll(Arb.string(1..20)) { entityId ->
                val registry = InMemoryEntityRegistry()
                registry.getOrCreate(entityId)

                registry.remove(entityId)

                val snapshot = registry.snapshot()
                snapshot.map { it.entityId } shouldNotContain entityId
            }
        }
    })
