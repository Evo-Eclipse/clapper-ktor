package com.example.domain

import com.example.domain.AnimEvent.ATTACK_INPUT
import com.example.domain.AnimEvent.HIT
import com.example.domain.AnimEvent.MOVE
import com.example.domain.AnimEvent.RESPAWN
import com.example.domain.AnimEvent.SPRINT
import com.example.domain.AnimEvent.STOP
import com.example.domain.AnimState.ATTACK
import com.example.domain.AnimState.DEATH
import com.example.domain.AnimState.IDLE
import com.example.domain.AnimState.RUN
import com.example.domain.AnimState.WALK

object TransitionTable {
    private val table: Map<Pair<AnimState, AnimEvent>, AnimState> =
        mapOf(
            (IDLE to MOVE) to WALK,
            (IDLE to ATTACK_INPUT) to ATTACK,
            (IDLE to HIT) to DEATH,
            (WALK to SPRINT) to RUN,
            (WALK to STOP) to IDLE,
            (WALK to ATTACK_INPUT) to ATTACK,
            (WALK to HIT) to DEATH,
            (RUN to STOP) to WALK,
            (RUN to HIT) to DEATH,
            (ATTACK to MOVE) to IDLE,
            (ATTACK to HIT) to DEATH,
            (DEATH to RESPAWN) to IDLE,
        )

    /**
     * Возвращает целевое состояние или null, если переход запрещён.
     * Чистая функция — без side-effects.
     */
    fun transition(
        current: AnimState,
        event: AnimEvent,
    ): AnimState? = table[current to event]

    /** Список разрешённых событий для данного состояния */
    fun allowedEvents(state: AnimState): Set<AnimEvent> =
        table.keys
            .filter { it.first == state }
            .map { it.second }
            .toSet()

    /** Все валидные пары (state, event) — для генераторов тестов */
    fun allValidPairs(): List<Pair<AnimState, AnimEvent>> = table.keys.toList()
}
