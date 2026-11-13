package com.jjmluz.nationslingo.wear

import com.jjmluz.nationslingo.Country
import com.jjmluz.nationslingo.Direction
import com.jjmluz.nationslingo.Round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CrownTest {
    private fun country(code: String) = Country(code, 0, 0)

    private val round = Round(
        target = country("pt"),
        options = mapOf(
            Direction.UP to country("pt"),
            Direction.RIGHT to country("es"),
            Direction.DOWN to country("fr"),
            Direction.LEFT to country("de"),
        ),
    )

    @Test
    fun `nothing is highlighted until the crown turns`() {
        assertNull(Crown().highlighted(round))
    }

    @Test
    fun `turning forward goes clockwise from the first answer and wraps round`() {
        val crown = Crown()
        val seen = (1..5).map { crown.move(round, forward = true); crown.highlighted(round) }
        assertEquals(ANSWER_ORDER + ANSWER_ORDER.first(), seen)
    }

    @Test
    fun `turning back starts from the last answer`() {
        val crown = Crown()
        crown.move(round, forward = false)
        assertEquals(Direction.LEFT, crown.highlighted(round))
    }

    @Test
    fun `answers ruled out are skipped`() {
        val crown = Crown()
        val narrowed = round.copy(eliminated = setOf(Direction.UP, Direction.DOWN))
        crown.move(narrowed, forward = true)
        assertEquals(Direction.RIGHT, crown.highlighted(narrowed))
        crown.move(narrowed, forward = true)
        assertEquals(Direction.LEFT, crown.highlighted(narrowed))
        crown.move(narrowed, forward = true)
        assertEquals(Direction.RIGHT, crown.highlighted(narrowed))
    }

    @Test
    fun `a new country starts with nothing highlighted`() {
        val crown = Crown()
        crown.move(round, forward = true)
        assertNull(crown.highlighted(round.copy(target = country("es"))))
    }

    @Test
    fun `with every answer ruled out the crown does nothing`() {
        assertFalse(Crown().move(round.copy(eliminated = ANSWER_ORDER.toSet()), forward = true))
    }
}
