package com.example.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.forAll

/**
 * Property-based tests for StateMachine.
 *
 * Validates: Requirements 1.2, 2.6
 */
class StateMachinePropertyTest :
    FunSpec({

        test(
            "Property 3: Новые сущности начинают с IDLE — " +
                "for any entityId, a new StateMachine has current == IDLE",
        ) {
            /**
             * Validates: Requirements 1.2, 2.6
             *
             * For any string entityId, when a new StateMachine is created,
             * its current state SHALL be IDLE.
             */
            forAll(Arb.string(1..100)) { entityId ->
                val machine = StateMachine(entityId)
                machine.current == AnimState.IDLE
            }
        }

        test("process() on forbidden transition returns Result.failure with InvalidTransitionException") {
            /**
             * Validates: Requirements 1.3, 2.7
             *
             * Unit test: when process() is called with an event that is not allowed
             * from the current state, it returns Result.failure containing
             * InvalidTransitionException, and the state remains unchanged.
             */
            checkAll(Arb.invalidTransition()) { (state, event) ->
                val machine = StateMachine("test-entity")
                // Drive machine to the desired state via valid transitions
                driveTo(machine, state)

                val stateBefore = machine.current
                val result = machine.process(event)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<InvalidTransitionException>()
                machine.current shouldBe stateBefore
            }
        }
    })

/**
 * Helper: drives a StateMachine from IDLE to the target state using known valid paths.
 */
private fun driveTo(
    machine: StateMachine,
    target: AnimState,
) {
    val path = pathTo(target)
    for (event in path) {
        machine.process(event).getOrThrow()
    }
}

/**
 * Returns a sequence of events to transition from IDLE to the given state.
 */
private fun pathTo(target: AnimState): List<AnimEvent> =
    when (target) {
        AnimState.IDLE -> emptyList()
        AnimState.WALK -> listOf(AnimEvent.MOVE)
        AnimState.RUN -> listOf(AnimEvent.MOVE, AnimEvent.SPRINT)
        AnimState.ATTACK -> listOf(AnimEvent.ATTACK_INPUT)
        AnimState.DEATH -> listOf(AnimEvent.HIT)
    }
