package com.jjmluz.nationslingo.wear

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jjmluz.nationslingo.Country
import com.jjmluz.nationslingo.Direction
import com.jjmluz.nationslingo.Round

/**
 * The order the answers sit in on the watch: clockwise round the grid of flags from the top left, or top
 * to bottom in the list of names. The crown moves through them in this order.
 */
val ANSWER_ORDER = listOf(Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT)

/** The answer the crown has highlighted. It starts with none, and again on every new country. */
class Crown {
    private var target by mutableStateOf<Country?>(null)
    private var index by mutableStateOf(0)

    fun highlighted(round: Round): Direction? = if (round.target == target) ANSWER_ORDER[index] else null

    /** Moves to the next answer not yet ruled out, or the previous one. Returns false if there's none. */
    fun move(round: Round, forward: Boolean): Boolean {
        val open = ANSWER_ORDER.indices.filter { ANSWER_ORDER[it] !in round.eliminated }
        if (open.isEmpty()) return false
        val current = if (round.target == target) index else null
        index = when {
            current == null -> if (forward) open.first() else open.last()
            forward -> open.firstOrNull { it > current } ?: open.first()
            else -> open.lastOrNull { it < current } ?: open.last()
        }
        target = round.target
        return true
    }
}
