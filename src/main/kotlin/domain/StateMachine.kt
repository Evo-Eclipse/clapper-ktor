package com.example.domain

/**
 * Mutable state machine for a single entity.
 * Processes events and transitions between animation states
 * according to the TransitionTable.
 *
 * Thread-safety: callers should synchronize on this instance
 * when invoking [process] from multiple threads.
 */
class StateMachine(
    val entityId: String,
) {
    @Volatile
    var current: AnimState = AnimState.IDLE
        private set

    /**
     * Processes an event and attempts a state transition.
     *
     * @return [Result.success] with a pair (fromState, toState) on valid transition,
     *         or [Result.failure] with [InvalidTransitionException] if the transition is forbidden.
     */
    fun process(event: AnimEvent): Result<Pair<AnimState, AnimState>> {
        val from = current
        val to =
            TransitionTable.transition(from, event)
                ?: return Result.failure(
                    InvalidTransitionException(entityId, from.name, event.name),
                )
        current = to
        return Result.success(from to to)
    }
}
