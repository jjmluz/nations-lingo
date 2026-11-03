package com.jjmluz.bandeiras

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlin.random.Random

const val LOW_TIME_SECONDS = 10

private const val RIGHT_ANSWER_MILLIS = 400L

enum class Screen { START, PLAYING, PAUSED, OVER }

enum class Mode(val title: String, val hint: String, val bestKey: String) {
    GUESS_FLAG("Adivinha a Bandeira", "Vê o país e escolhe a bandeira", "best"),
    GUESS_COUNTRY("Adivinha o País", "Vê a bandeira e escolhe o país", "best_country"),
}

/** The last time bonus or penalty, new on each answer so the UI can replay its animation. */
class TimeChange(val correct: Boolean)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("bandeiras", Context.MODE_PRIVATE)
    private val sounds = Sounds(app)
    private val bests = mutableStateMapOf<Mode, Int>().apply {
        Mode.entries.forEach { put(it, prefs.getInt(it.bestKey, 0)) }
    }
    private var game: Game? = null
    private var rightAnswerUntil = 0L

    var screen by mutableStateOf(Screen.START)
        private set
    var mode by mutableStateOf(Mode.GUESS_FLAG)
        private set
    var round by mutableStateOf<Round?>(null)
        private set
    /** The side of a right answer being held on screen, with the clock frozen, before the next round. */
    var rightAnswer by mutableStateOf<Direction?>(null)
        private set
    var score by mutableIntStateOf(0)
        private set
    var secondsLeft by mutableIntStateOf(0)
        private set
    var isNewBest by mutableStateOf(false)
        private set
    var timeChange by mutableStateOf<TimeChange?>(null)
        private set

    fun best(mode: Mode): Int = bests.getValue(mode)

    /** Returns true when the key was used, so it doesn't also reach the system. */
    fun onKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> answer(Direction.UP)
        KeyEvent.KEYCODE_DPAD_DOWN -> answer(Direction.DOWN)
        KeyEvent.KEYCODE_DPAD_LEFT -> answer(Direction.LEFT)
        KeyEvent.KEYCODE_DPAD_RIGHT -> answer(Direction.RIGHT)
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> confirm()
        KeyEvent.KEYCODE_BACK -> back()
        else -> false
    }

    fun tick() {
        val current = game ?: return
        if (screen != Screen.PLAYING) return
        val now = SystemClock.elapsedRealtime()
        if (rightAnswer != null) {
            if (now >= rightAnswerUntil) endRightAnswer(current, now)
        } else if (current.isOver(now)) {
            finish(current)
        } else {
            val before = secondsLeft
            show(current, now)
            if (secondsLeft < before && secondsLeft <= LOW_TIME_SECONDS) sounds.play(Sound.TICK)
        }
    }

    fun pause() {
        if (screen == Screen.PLAYING) {
            val current = game ?: return
            val now = SystemClock.elapsedRealtime()
            rightAnswer = null
            if (current.isOver(now)) {
                finish(current)
            } else {
                current.pause(now)
                show(current, now)
                screen = Screen.PAUSED
            }
        }
    }

    override fun onCleared() = sounds.release()

    private fun chooseMode(direction: Direction): Boolean {
        val chosen = when (direction) {
            Direction.LEFT -> Mode.GUESS_FLAG
            Direction.RIGHT -> Mode.GUESS_COUNTRY
            Direction.UP, Direction.DOWN -> return false
        }
        if (chosen != mode) {
            mode = chosen
            sounds.play(Sound.MOVE)
        }
        return true
    }

    private fun answer(direction: Direction): Boolean {
        if (screen == Screen.START) return chooseMode(direction)
        val current = game
        if (current == null || screen != Screen.PLAYING) return false
        if (rightAnswer != null) return true
        val now = SystemClock.elapsedRealtime()
        val outcome = current.answer(direction, now)
        when (outcome) {
            Outcome.CORRECT_FIRST_TRY -> {
                timeChange = TimeChange(correct = true)
                startRightAnswer(current, direction, now)
            }
            Outcome.CORRECT_RETRY -> startRightAnswer(current, direction, now)
            Outcome.WRONG -> {
                timeChange = TimeChange(correct = false)
                sounds.play(Sound.WRONG)
            }
            Outcome.IGNORED -> Unit
        }
        if (rightAnswer == null) {
            if (current.isOver(now)) finish(current) else show(current, now)
        }
        return true
    }

    private fun startRightAnswer(current: Game, direction: Direction, now: Long) {
        sounds.play(Sound.CORRECT)
        current.pause(now)
        rightAnswer = direction
        rightAnswerUntil = now + RIGHT_ANSWER_MILLIS
        showStats(current, now)
    }

    private fun endRightAnswer(current: Game, now: Long) {
        rightAnswer = null
        current.resume(now)
        if (current.isOver(now)) finish(current) else show(current, now)
    }

    private fun confirm(): Boolean {
        when (screen) {
            Screen.START, Screen.OVER -> start()
            Screen.PAUSED -> resume()
            Screen.PLAYING -> Unit
        }
        return true
    }

    private fun back(): Boolean {
        when (screen) {
            Screen.START -> return false
            Screen.PLAYING -> pause()
            Screen.PAUSED, Screen.OVER -> screen = Screen.START
        }
        return true
    }

    private fun start() {
        val now = SystemClock.elapsedRealtime()
        val newGame = Game(COUNTRIES, Random.Default, now)
        game = newGame
        isNewBest = false
        timeChange = null
        show(newGame, now)
        screen = Screen.PLAYING
        sounds.play(Sound.START)
    }

    private fun resume() {
        val current = game ?: return
        val now = SystemClock.elapsedRealtime()
        current.resume(now)
        show(current, now)
        screen = Screen.PLAYING
    }

    private fun finish(finished: Game) {
        score = finished.score
        secondsLeft = 0
        if (finished.score > best(mode)) {
            bests[mode] = finished.score
            isNewBest = true
            prefs.edit { putInt(mode.bestKey, finished.score) }
        }
        screen = Screen.OVER
        sounds.play(if (finished.round == null) Sound.VICTORY else Sound.GAME_OVER)
    }

    private fun show(current: Game, now: Long) {
        round = current.round
        showStats(current, now)
    }

    private fun showStats(current: Game, now: Long) {
        score = current.score
        secondsLeft = ((current.timeLeft(now) + 999) / 1000).toInt()
    }
}
