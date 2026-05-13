package com.example.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
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

fun Arb.Companion.stateChangedDto(): Arb<StateChangedDto> =
    Arb.bind(
        Arb.string(1..50),
        Arb.enum<AnimState>(),
        Arb.enum<AnimState>(),
        Arb.enum<AnimEvent>(),
        Arb.long(0L..Long.MAX_VALUE / 2),
    ) { entityId, from, to, event, ts ->
        StateChangedDto(entityId, from.name, to.name, event.name, ts)
    }

fun Arb.Companion.entitySnapshotDto(): Arb<EntitySnapshotDto> =
    Arb.bind(
        Arb.string(1..50),
        Arb.enum<AnimState>(),
    ) { entityId, state ->
        EntitySnapshotDto(entityId, state.name)
    }

fun Arb.Companion.invalidClipCreateRequest(): Arb<ClipCreateRequest> =
    Arb.choice(
        // blank name (empty or whitespace only)
        Arb.bind(clipCreateRequest(), Arb.int(0..5)) { req, spaces ->
            req.copy(name = " ".repeat(spaces))
        },
        // name > 255 characters
        clipCreateRequest().map { it.copy(name = "x".repeat(256)) },
        // durationMs < 1
        Arb.bind(clipCreateRequest(), Arb.int(-1000..0)) { req, dur ->
            req.copy(durationMs = dur)
        },
        // durationMs > 600_000
        clipCreateRequest().map { it.copy(durationMs = 600_001) },
        // tags > 20
        clipCreateRequest().map { it.copy(tags = List(21) { "tag$it" }) },
        // tag length > 50
        clipCreateRequest().map { it.copy(tags = listOf("x".repeat(51))) },
    )
