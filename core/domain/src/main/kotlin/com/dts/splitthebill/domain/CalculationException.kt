package com.dts.splitthebill.domain

const val MAX_SAFE_INTEGER: Long = 9_007_199_254_740_991L

enum class CalculationErrorCode {
    INVALID_AMOUNT,
    INVALID_CURRENCY,
    INVALID_PARTICIPANT,
    INVALID_SPLIT,
    DUPLICATE_PARTICIPANT,
    CONTRIBUTIONS_DO_NOT_SUM,
    ALLOCATIONS_DO_NOT_SUM,
    BALANCES_NOT_CONSERVED,
    OVERFLOW,
}

class CalculationException(
    val code: CalculationErrorCode,
    message: String,
) : IllegalArgumentException(message)

internal object CalculationChecks {
    private val participantIdPattern = Regex("^[A-Za-z0-9_-]{1,128}$")

    fun participantId(value: String): String {
        if (!participantIdPattern.matches(value)) {
            fail(CalculationErrorCode.INVALID_PARTICIPANT, "Invalid participant ID")
        }
        return value
    }

    fun safeNonNegative(value: Long, field: String): Long {
        if (value !in 0..MAX_SAFE_INTEGER) {
            fail(CalculationErrorCode.INVALID_AMOUNT, "$field must be a non-negative safe integer")
        }
        return value
    }

    fun safePositive(value: Long, field: String): Long {
        safeNonNegative(value, field)
        if (value == 0L) fail(CalculationErrorCode.INVALID_AMOUNT, "$field must be positive")
        return value
    }

    fun safeSigned(value: Long, field: String): Long {
        if (value !in -MAX_SAFE_INTEGER..MAX_SAFE_INTEGER) {
            fail(CalculationErrorCode.OVERFLOW, "$field is outside the safe integer range")
        }
        return value
    }

    fun add(left: Long, right: Long, field: String): Long {
        safeSigned(left, field)
        safeSigned(right, field)
        if (right > 0 && left > MAX_SAFE_INTEGER - right) {
            fail(CalculationErrorCode.OVERFLOW, "$field exceeds the safe integer range")
        }
        if (right < 0 && left < -MAX_SAFE_INTEGER - right) {
            fail(CalculationErrorCode.OVERFLOW, "$field exceeds the safe integer range")
        }
        return left + right
    }

    fun negate(value: Long, field: String): Long {
        safeSigned(value, field)
        return -value
    }

    fun uniqueParticipantIds(ids: List<String>) {
        if (ids.isEmpty() || ids.size > 100) {
            fail(CalculationErrorCode.INVALID_SPLIT, "A split needs between 1 and 100 participants")
        }
        ids.forEach(::participantId)
        if (ids.toSet().size != ids.size) {
            fail(CalculationErrorCode.DUPLICATE_PARTICIPANT, "Participant IDs must be unique")
        }
    }

    fun fail(code: CalculationErrorCode, message: String): Nothing =
        throw CalculationException(code, message)
}
