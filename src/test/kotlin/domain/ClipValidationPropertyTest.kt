package com.example.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.checkAll

/**
 * Property-based tests for clip validation.
 *
 * Validates: Requirements 5.2, 7.3
 */
class ClipValidationPropertyTest :
    FunSpec({

        test(
            "Property 10: Валидация отклоняет невалидные данные клипа — " +
                "for any invalid request, validateClipCreate returns non-empty error list",
        ) {
            /**
             * Validates: Requirements 5.2, 7.3
             *
             * For any ClipCreateRequest violating at least one validation rule,
             * validateClipCreate() SHALL return a non-empty list of errors.
             */
            checkAll(Arb.invalidClipCreateRequest()) { request ->
                val errors = validateClipCreate(request)
                errors.shouldNotBeEmpty()
            }
        }

        test(
            "Property 10: Валидация принимает валидные данные клипа — " +
                "for any valid request, validateClipCreate returns empty error list",
        ) {
            /**
             * Validates: Requirements 5.2, 7.3
             *
             * For any valid ClipCreateRequest (generated within valid bounds),
             * validateClipCreate() SHALL return an empty list of errors.
             */
            checkAll(Arb.clipCreateRequest()) { request ->
                val errors = validateClipCreate(request)
                errors.shouldBeEmpty()
            }
        }
    })
