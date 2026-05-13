package com.example.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter

fun Arb.Companion.animState(): Arb<AnimState> = Arb.enum<AnimState>()

fun Arb.Companion.animEvent(): Arb<AnimEvent> = Arb.enum<AnimEvent>()

fun Arb.Companion.validTransition(): Arb<Pair<AnimState, AnimEvent>> =
    Arb.element(TransitionTable.allValidPairs())

fun Arb.Companion.invalidTransition(): Arb<Pair<AnimState, AnimEvent>> =
    Arb.bind(animState(), animEvent()) { state, event -> state to event }
        .filter { TransitionTable.transition(it.first, it.second) == null }
