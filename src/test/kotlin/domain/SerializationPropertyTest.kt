package com.example.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import kotlinx.serialization.json.Json

/**
 * Property-based tests for DTO serialization round-trip.
 *
 * Validates: Requirements 13.4
 */
class SerializationPropertyTest :
    FunSpec({

        val json = Json

        test(
            "Property 11: Сериализация round-trip для StateChangedDto — " +
                "serialize then deserialize equals original",
        ) {
            /**
             * Validates: Requirements 13.4
             *
             * For any valid StateChangedDto, serializing to JSON and
             * deserializing back SHALL produce an object equal to the original.
             */
            checkAll(Arb.stateChangedDto()) { dto ->
                val serialized = json.encodeToString(StateChangedDto.serializer(), dto)
                val deserialized = json.decodeFromString(StateChangedDto.serializer(), serialized)
                deserialized shouldBe dto
            }
        }

        test(
            "Property 11: Сериализация round-trip для AnimationClip — " +
                "serialize then deserialize equals original",
        ) {
            /**
             * Validates: Requirements 13.4
             *
             * For any valid AnimationClip, serializing to JSON and
             * deserializing back SHALL produce an object equal to the original.
             */
            checkAll(Arb.animationClip()) { clip ->
                val serialized = json.encodeToString(AnimationClip.serializer(), clip)
                val deserialized = json.decodeFromString(AnimationClip.serializer(), serialized)
                deserialized shouldBe clip
            }
        }

        test(
            "Property 11: Сериализация round-trip для EntitySnapshotDto — " +
                "serialize then deserialize equals original",
        ) {
            /**
             * Validates: Requirements 13.4
             *
             * For any valid EntitySnapshotDto, serializing to JSON and
             * deserializing back SHALL produce an object equal to the original.
             */
            checkAll(Arb.entitySnapshotDto()) { dto ->
                val serialized = json.encodeToString(EntitySnapshotDto.serializer(), dto)
                val deserialized = json.decodeFromString(EntitySnapshotDto.serializer(), serialized)
                deserialized shouldBe dto
            }
        }
    })
