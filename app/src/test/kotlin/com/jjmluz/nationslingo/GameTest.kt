package com.jjmluz.nationslingo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private fun newGame(seed: Int = 1) = Game(COUNTRIES, Random(seed), now = 0)

    private fun Game.rightDirection(): Direction =
        round!!.options.entries.first { it.value == round!!.target }.key

    private fun Game.wrongDirection(): Direction =
        round!!.options.entries.first { it.value != round!!.target && it.key !in round!!.eliminated }.key

    @Test
    fun `has every country once, each with its own flag`() {
        assertEquals(197, COUNTRIES.size)
        assertEquals(197, COUNTRIES.map { it.code }.toSet().size)
        assertEquals(197, COUNTRIES.map { it.name }.toSet().size)
        assertEquals(197, COUNTRIES.map { it.flag }.toSet().size)
    }

    @Test
    fun `starts with one minute and counts down in real time`() {
        val game = newGame()
        assertEquals(60_000, game.timeLeft(0))
        assertEquals(59_000, game.timeLeft(1_000))
        assertEquals(15_000, game.timeLeft(45_000))
    }

    @Test
    fun `right answer on the first try scores, adds two seconds and moves to the next country`() {
        val game = newGame()
        val target = game.round!!.target
        assertEquals(Outcome.CORRECT_FIRST_TRY, game.answer(game.rightDirection(), 10_000))
        assertEquals(1, game.score)
        assertEquals(52_000, game.timeLeft(10_000))
        assertNotEquals(target, game.round!!.target)
    }

    @Test
    fun `wrong answer costs two seconds and stays on the same country`() {
        val game = newGame()
        val target = game.round!!.target
        val wrong = game.wrongDirection()
        assertEquals(Outcome.WRONG, game.answer(wrong, 10_000))
        assertEquals(0, game.score)
        assertEquals(48_000, game.timeLeft(10_000))
        assertEquals(target, game.round!!.target)
        assertEquals(setOf(wrong), game.round!!.eliminated)
    }

    @Test
    fun `picking a rejected flag again costs nothing`() {
        val game = newGame()
        val wrong = game.wrongDirection()
        game.answer(wrong, 0)
        assertEquals(Outcome.IGNORED, game.answer(wrong, 0))
        assertEquals(58_000, game.timeLeft(0))
    }

    @Test
    fun `right answer after a wrong one scores and moves on but adds no time`() {
        val game = newGame()
        val target = game.round!!.target
        game.answer(game.wrongDirection(), 0)
        assertEquals(Outcome.CORRECT_RETRY, game.answer(game.rightDirection(), 0))
        assertEquals(1, game.score)
        assertEquals(58_000, game.timeLeft(0))
        assertNotEquals(target, game.round!!.target)
    }

    @Test
    fun `right answer still works after all wrong ones, with no bonus`() {
        val game = newGame()
        repeat(3) { assertEquals(Outcome.WRONG, game.answer(game.wrongDirection(), 0)) }
        assertEquals(54_000, game.timeLeft(0))
        assertEquals(Outcome.CORRECT_RETRY, game.answer(game.rightDirection(), 0))
        assertEquals(54_000, game.timeLeft(0))
    }

    @Test
    fun `a retry doesn't affect the bonus on the next country`() {
        val game = newGame()
        game.answer(game.wrongDirection(), 0)
        game.answer(game.rightDirection(), 0)
        assertEquals(Outcome.CORRECT_FIRST_TRY, game.answer(game.rightDirection(), 0))
        assertEquals(60_000, game.timeLeft(0))
    }

    @Test
    fun `pressing a rejected flag again doesn't bring the bonus back`() {
        val game = newGame()
        val wrong = game.wrongDirection()
        game.answer(wrong, 0)
        game.answer(wrong, 0)
        assertEquals(Outcome.CORRECT_RETRY, game.answer(game.rightDirection(), 0))
        assertEquals(58_000, game.timeLeft(0))
    }

    @Test
    fun `lookalike pairs in the game use codes from the country list`() {
        val codes = COUNTRIES.map { it.code }.toSet()
        assertTrue(LOOKALIKES.flatten().all { it in codes })
    }

    @Test
    fun `every round has four different flags on four sides including the target`() {
        for (seed in 0 until 50) {
            val game = newGame(seed)
            while (game.round != null) {
                val round = game.round!!
                assertEquals(Direction.entries.toSet(), round.options.keys)
                assertEquals(4, round.options.values.toSet().size)
                assertTrue(round.target in round.options.values)
                game.answer(game.rightDirection(), 0)
            }
        }
    }

    @Test
    fun `the right flag shows up on every side`() {
        val game = newGame()
        val sides = mutableSetOf<Direction>()
        while (game.round != null) {
            sides += game.rightDirection()
            game.answer(game.rightDirection(), 0)
        }
        assertEquals(Direction.entries.toSet(), sides)
    }

    @Test
    fun `lookalike flags never share a round`() {
        for (seed in 0 until 200) {
            val game = newGame(seed)
            while (game.round != null) {
                val codes = game.round!!.options.values.map { it.code }
                LOOKALIKES.forEach { pair -> assertFalse(codes.containsAll(pair)) }
                game.answer(game.rightDirection(), 0)
            }
        }
    }

    @Test
    fun `game ends when the clock reaches zero`() {
        val game = newGame()
        assertFalse(game.isOver(59_999))
        assertTrue(game.isOver(60_000))
        assertEquals(0, game.timeLeft(99_000))
    }

    @Test
    fun `bonus extends the deadline`() {
        val game = newGame()
        game.answer(game.rightDirection(), 59_000)
        assertFalse(game.isOver(60_000))
        assertEquals(1_000, game.timeLeft(61_000))
        assertTrue(game.isOver(62_000))
    }

    @Test
    fun `wrong answer with under two seconds left ends the game`() {
        val game = newGame()
        game.answer(game.wrongDirection(), 58_500)
        assertTrue(game.isOver(58_500))
        assertEquals(0, game.timeLeft(58_500))
    }

    @Test
    fun `answers after time is up are ignored`() {
        val game = newGame()
        assertEquals(Outcome.IGNORED, game.answer(game.rightDirection(), 60_000))
        assertEquals(0, game.score)
    }

    @Test
    fun `pausing freezes the clock and blocks answers`() {
        val game = newGame()
        game.pause(10_000)
        assertEquals(50_000, game.timeLeft(500_000))
        assertFalse(game.isOver(500_000))
        assertEquals(Outcome.IGNORED, game.answer(game.rightDirection(), 20_000))
        game.resume(500_000)
        assertEquals(49_000, game.timeLeft(501_000))
    }

    @Test
    fun `pausing twice keeps the time from the first pause`() {
        val game = newGame()
        game.pause(10_000)
        game.pause(20_000)
        game.resume(30_000)
        assertEquals(50_000, game.timeLeft(30_000))
    }

    @Test
    fun `guessing every country asks each one once and ends the game`() {
        val game = newGame()
        val asked = mutableListOf<Country>()
        while (game.round != null) {
            asked += game.round!!.target
            game.answer(game.rightDirection(), 0)
        }
        assertEquals(COUNTRIES.toSet(), asked.toSet())
        assertEquals(197, asked.size)
        assertEquals(197, game.score)
        assertNull(game.round)
        assertTrue(game.isOver(0))
    }

    @Test
    fun `separate games use a different order`() {
        assertNotNull(newGame(1).round)
        assertNotEquals(newGame(1).round!!.target, newGame(2).round!!.target)
    }
}
