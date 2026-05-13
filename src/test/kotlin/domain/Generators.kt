package com.example.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid

fun Arb.Companion.animState(): Arb<AnimState> = Arb.enum<AnimState>()

fun Arb.Companion.animEvent(): Arb<AnimEvent> = Arb.enum<AnimEvent>()

fun Arb.Companion.validTransition(): Arb<Pair<AnimState, AnimEvent>> = Arb.element(TransitionTable.allValidPairs())

fun Arb.Companion.invalidTransition(): Arb<Pair<AnimState, AnimEvent>> =
    Arb
        .bind(animState(), animEvent()) { state, event -> state to event }
        .filter { TransitionTable.transition(it.first, it.second) == null }

fun Arb.Companion.clipCreateRequest(): Arb<ClipCreateRequest> =
    Arb.bind(
        Arb.string(1..50),
        Arb.enum<AnimState>(),
        Arb.int(1..600_000),
        Arb.boolean(),
        Arb.list(Arb.string(1..20), 0..5),
    ) { name, state, duration, loop, tags ->
        ClipCreateRequest(name, state, duration, loop, tags)
    }

fun Arb.Companion.animationClip(): Arb<AnimationClip> =
    Arb.bind(
        Arb.uuid().map { it.toString() },
        Arb.string(1..50),
        Arb.enum<AnimState>(),
        Arb.int(1..600_000),
        Arb.boolean(),
        Arb.list(Arb.string(1..20), 0..5),
    ) { id, name, state, duration, loop, tags ->
        AnimationClip(id, name, state, duration, loop, tags)
    }
