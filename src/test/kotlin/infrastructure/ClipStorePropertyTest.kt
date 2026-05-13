package com.example.infrastructure

import com.example.domain.AnimState
import com.example.domain.animationClip
import com.example.domain.clipCreateRequest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

/**
 * Property-based tests for ClipStore.
 *
 * Validates: Requirements 5.1, 5.3, 6.2, 6.3, 7.1, 7.4, 8.1, 8.3
 */
class ClipStorePropertyTest :
    FunSpec({

        test(
            "Property 6: Round-trip создания и получения клипа — " +
                "create then getById returns same object except id",
        ) {
            /**
             * Validates: Requirements 5.1, 5.3, 6.3
             *
             * For any valid ClipCreateRequest, after creating a clip via create(),
             * getById() with the returned id SHALL return an object with the same
             * field values (name, state, durationMs, loop, tags), and the id SHALL
             * be a valid UUID different from any passed in the request.
             */
            checkAll(Arb.clipCreateRequest()) { request ->
                val store = InMemoryClipStore()
                val created = store.create(request.toAnimationClip())

                created.id.isNotBlank() shouldBe true
                created.name shouldBe request.name
                created.state shouldBe request.state
                created.durationMs shouldBe request.durationMs
                created.loop shouldBe request.loop
                created.tags shouldBe request.tags

                val retrieved = store.getById(created.id)
                retrieved.shouldNotBeNull()
                retrieved shouldBe created
            }
        }

        test(
            "Property 7: Фильтрация клипов по состоянию — " +
                "getByState returns only clips with matching state",
        ) {
            /**
             * Validates: Requirements 6.2
             *
             * For any set of clips in ClipStore and any AnimState value,
             * getByState(state) SHALL return only clips whose state field
             * equals the specified value, and SHALL return all such clips.
             */
            checkAll(
                Arb.list(Arb.clipCreateRequest(), 1..10),
                Arb.enum<AnimState>(),
            ) { requests, filterState ->
                val store = InMemoryClipStore()
                val created = requests.map { store.create(it.toAnimationClip()) }

                val filtered = store.getByState(filterState)
                val expected = created.filter { it.state == filterState }

                filtered.size shouldBe expected.size
                filtered.all { it.state == filterState } shouldBe true
                filtered shouldContainAll expected
            }
        }

        test(
            "Property 8: Обновление клипа полностью заменяет данные — " +
                "update replaces all data, id from URL preserved",
        ) {
            /**
             * Validates: Requirements 7.1, 7.4
             *
             * For any existing clip with id and any valid AnimationClip update,
             * after update(id, clip), getById SHALL return an object with data
             * from the update and the id from the URL, regardless of the id in the body.
             */
            checkAll(
                Arb.clipCreateRequest(),
                Arb.animationClip(),
            ) { createRequest, updateData ->
                val store = InMemoryClipStore()
                val created = store.create(createRequest.toAnimationClip())

                val updated = store.update(created.id, updateData)
                updated.shouldNotBeNull()
                updated.id shouldBe created.id
                updated.name shouldBe updateData.name
                updated.state shouldBe updateData.state
                updated.durationMs shouldBe updateData.durationMs
                updated.loop shouldBe updateData.loop
                updated.tags shouldBe updateData.tags

                val retrieved = store.getById(created.id)
                retrieved shouldBe updated
            }
        }

        test(
            "Property 9: Удаление клипа делает его недоступным — " +
                "after delete, getById returns null",
        ) {
            /**
             * Validates: Requirements 8.1, 8.3
             *
             * For any existing clip with id, after delete(id),
             * getById(id) SHALL return null.
             */
            checkAll(Arb.clipCreateRequest()) { request ->
                val store = InMemoryClipStore()
                val created = store.create(request.toAnimationClip())

                store.delete(created.id) shouldBe true
                store.getById(created.id).shouldBeNull()
            }
        }
    })
