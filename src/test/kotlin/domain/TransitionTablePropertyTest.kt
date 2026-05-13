package com.example.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.forAll

/**
 * Property-based tests for TransitionTable.
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.7, 2.8, 1.1, 1.3
 */
class TransitionTablePropertyTest :
    FunSpec({

        test(
            "Property 1: Допустимые переходы FSM соответствуют таблице — " +
                "for any valid pair (state, event), transition() always returns the same target state",
        ) {
            /**
             * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.8, 1.1
             *
             * For any valid pair (state, event) from the transition table,
             * TransitionTable.transition(state, event) SHALL always return the same
             * target state defined by the table, regardless of the number of calls
             * and order of previous operations.
             */
            forAll(Arb.validTransition()) { (state, event) ->
                val result1 = TransitionTable.transition(state, event)
                val result2 = TransitionTable.transition(state, event)
                result1 != null && result1 == result2
            }
        }

        test(
            "Property 2: Запрещённые переходы сохраняют состояние — " +
                "for any invalid pair, transition() returns null",
        ) {
            /**
             * Validates: Requirements 1.3, 2.7
             *
             * For any pair (state, event) NOT in the transition table,
             * TransitionTable.transition(state, event) SHALL return null,
             * indicating the transition is forbidden.
             */
            forAll(Arb.invalidTransition()) { (state, event) ->
                TransitionTable.transition(state, event) == null
            }
        }

        test("Property 1 — determinism: calling transition multiple times yields identical results") {
            /**
             * Validates: Requirements 2.8
             *
             * Additional determinism check: calling transition() 10 times
             * for the same valid pair always returns the same result.
             */
            forAll(Arb.validTransition()) { (state, event) ->
                val expected = TransitionTable.transition(state, event)
                (1..10).all { TransitionTable.transition(state, event) == expected }
            }
        }

        test("Property 2 — completeness: all state-event pairs are either valid or invalid") {
            /**
             * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.7
             *
             * Every possible (AnimState, AnimEvent) pair is either in the valid
             * transitions table (returns non-null) or is invalid (returns null).
             * No pair can be both or neither.
             */
            val validPairs = TransitionTable.allValidPairs().toSet()
            for (state in AnimState.entries) {
                for (event in AnimEvent.entries) {
                    val result = TransitionTable.transition(state, event)
                    if ((state to event) in validPairs) {
                        result shouldBe TransitionTable.transition(state, event)
                    } else {
                        result.shouldBeNull()
                    }
                }
            }
        }
    })
