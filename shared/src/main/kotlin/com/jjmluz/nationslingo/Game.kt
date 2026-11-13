package com.jjmluz.nationslingo

import kotlin.random.Random

const val START_MILLIS = 60_000L
const val BONUS_MILLIS = 2_000L
const val PENALTY_MILLIS = 2_000L

// Flags that can't be told apart on screen, so they never appear in the same round.
internal val LOOKALIKES = setOf(
    setOf("td", "ro"), // Chad, Romania
    setOf("id", "mc"), // Indonesia, Monaco
)

enum class Direction { UP, DOWN, LEFT, RIGHT }

enum class Outcome { CORRECT_FIRST_TRY, CORRECT_RETRY, WRONG, IGNORED }

data class Round(
    val target: Country,
    val options: Map<Direction, Country>,
    val eliminated: Set<Direction> = emptySet(),
)

/** One run of the game. Times are milliseconds on any monotonic clock. */
class Game(
    private val countries: List<Country>,
    private val random: Random,
    now: Long,
) {
    private val queue = countries.shuffled(random).toMutableList()
    private var deadline = now + START_MILLIS
    private var pausedTimeLeft: Long? = null

    var score = 0
        private set

    /** Null once every country has been guessed. */
    var round: Round? = nextRound()
        private set

    val isPaused get() = pausedTimeLeft != null

    fun timeLeft(now: Long): Long = (pausedTimeLeft ?: (deadline - now)).coerceAtLeast(0)

    fun isOver(now: Long): Boolean = round == null || timeLeft(now) == 0L

    fun answer(direction: Direction, now: Long): Outcome {
        val current = round
        if (current == null || isPaused || isOver(now) || direction in current.eliminated) {
            return Outcome.IGNORED
        }
        return if (current.options.getValue(direction) == current.target) {
            val firstTry = current.eliminated.isEmpty()
            score++
            if (firstTry) deadline += BONUS_MILLIS
            round = nextRound()
            if (firstTry) Outcome.CORRECT_FIRST_TRY else Outcome.CORRECT_RETRY
        } else {
            deadline -= PENALTY_MILLIS
            round = current.copy(eliminated = current.eliminated + direction)
            Outcome.WRONG
        }
    }

    fun pause(now: Long) {
        if (!isPaused) pausedTimeLeft = timeLeft(now)
    }

    fun resume(now: Long) {
        pausedTimeLeft?.let { deadline = now + it }
        pausedTimeLeft = null
    }

    private fun nextRound(): Round? {
        val target = queue.removeLastOrNull() ?: return null
        val picked = mutableListOf(target)
        for (country in countries.shuffled(random)) {
            if (picked.size == Direction.entries.size) break
            if (picked.none { it == country || setOf(it.code, country.code) in LOOKALIKES }) {
                picked += country
            }
        }
        return Round(target, Direction.entries.zip(picked.shuffled(random)).toMap())
    }
}
